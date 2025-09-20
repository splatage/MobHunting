package metadev.digital.MetaMobHunting.grinding;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.Messages.MessageHelper;

/**
 * Async worker that consumes kill snapshots and detects grinding areas.
 * It NEVER touches Bukkit API except via runTask(...) back on main thread.
 */
final class GrindingDetectionWorker {

    // Small immutable snapshot (no Bukkit refs)
    static final class KillRecord {
        final UUID worldId;
        final long timeMs;
        final double x, y, z;
        final EntityType type;
        final DamageCause cause;

        KillRecord(UUID worldId, long timeMs, double x, double y, double z, EntityType type, DamageCause cause) {
            this.worldId = worldId;
            this.timeMs = timeMs;
            this.x = x; this.y = y; this.z = z;
            this.type = type;
            this.cause = cause;
        }
    }

    static final class DetectionResult {
        final UUID worldId;
        final double x, y, z;
        final double radius;
        final int threshold;

        DetectionResult(UUID worldId, double x, double y, double z, double radius, int threshold) {
            this.worldId = worldId;
            this.x = x; this.y = y; this.z = z;
            this.radius = radius;
            this.threshold = threshold;
        }
    }

    private final MobHunting plugin;
    private final java.util.function.Consumer<DetectionResult> publishOnMain;
    private final ConcurrentLinkedQueue<KillRecord> queue = new ConcurrentLinkedQueue<>();
    // approximate, lock-free counter for queue length
    private final LongAdder qsize = new LongAdder();
    // Worker-owned index. Accessed only on the async scheduler thread.
    private final Map<UUID, Deque<KillRecord>> byWorld = new ConcurrentHashMap<>();

    // Limits
    private static final int BATCH_LIMIT = 512;     // drain per pass
    private static final int MAX_DEQUE_SIZE = 20000; // per world soft cap

    // Allow manager to clear a world's buffer on unload
    void clearWorld(UUID worldId) {
        if (worldId != null) {
            byWorld.remove(worldId);
        }
    }

    GrindingDetectionWorker(MobHunting plugin, java.util.function.Consumer<DetectionResult> publishOnMain) {
        this.plugin = plugin;
        this.publishOnMain = publishOnMain;
        // Kick off an async repeating task that processes the queue
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::process, 1L, 1L);
    }

    void offer(KillRecord r) {
        if (r == null) return;
        // never block the main thread; drop when above cap
        if (qsize.longValue() >= GLOBAL_QUEUE_MAX) return;
        queue.offer(r);
        qsize.increment();
    }

    // ----------------------------------------------------------------
    // Async loop (runs off the main thread)
    // ----------------------------------------------------------------
    private void process() {
        // Drain a bounded batch each tick to avoid long async bursts
        for (int i = 0; i < BATCH_LIMIT; i++) {
            KillRecord r = queue.poll();
            if (r == null) break;
            qsize.decrement();
            byWorld.computeIfAbsent(r.worldId, __ -> new ArrayDeque<>()).addLast(r);
        }

        if (byWorld.isEmpty()) return;

        long now = System.currentTimeMillis();

        // Read config (no Bukkit calls)
        var cfg = plugin.getConfigManager();

        // Windows/ranges
        final long enderSec      = cfg.secondsToSearchForGrindingOnEndermanFarms;
        final double enderRange  = cfg.rangeToSearchForGrindingOnEndermanFarms;
        final int enderThresh    = cfg.numberOfDeathsWhenSearchingForGringdingOnEndermanFarms;

        final long goldSec       = cfg.secondsToSearchForGrinding;
        final double goldRange   = cfg.rangeToSearchForGrinding;
        final int goldThresh     = cfg.numberOfDeathsWhenSearchingForGringding;

        final long otherSec      = cfg.secondsToSearchForGrindingOnOtherFarms;
        final double otherRange  = cfg.rangeToSearchForGrindingOnOtherFarms;
        final int otherThresh    = cfg.numberOfDeathsWhenSearchingForGringdingOnOtherFarms;

        // Process per world
        for (Map.Entry<UUID, Deque<KillRecord>> e : byWorld.entrySet()) {
            UUID wid = e.getKey();
            Deque<KillRecord> dq = e.getValue();
            if (dq.isEmpty()) continue;

            // Trim stale heads using the maximum window
            long maxWindowSec = Math.max(Math.max(enderSec, goldSec), otherSec);
            long cutoff = now - (maxWindowSec * 1000L);
            while (!dq.isEmpty()) {
                KillRecord head = dq.peekFirst();
                if (head == null || head.timeMs < cutoff) dq.pollFirst();
                else break;
            }
            // Soft cap to avoid unbounded growth on extreme bursts
            while (dq.size() > MAX_DEQUE_SIZE) dq.pollFirst();

            if (dq.isEmpty()) continue;

            // Scan newest -> older (short; we stop early by time)
            for (Iterator<KillRecord> it = dq.descendingIterator(); it.hasNext(); ) {
                KillRecord r = it.next();
                // Enderman VOID farm
                if (r.type == EntityType.ENDERMAN && r.cause == DamageCause.VOID && enderRange > 0 && enderSec > 0) {
                    if (countNearby(dq, r, enderRange, enderSec, now,
                            /*filter*/ (kr) -> kr.type == EntityType.ENDERMAN && kr.cause == DamageCause.VOID)
                        >= enderThresh) {
                        schedulePublish(new DetectionResult(wid, r.x, r.y, r.z, enderRange, enderThresh));
                    }
                }

                // Nether Gold XP farm (ZombiePigman FALL)
                if (isPigman(r.type) && r.cause == DamageCause.FALL && goldRange > 0 && goldSec > 0) {
                    if (countNearby(dq, r, goldRange, goldSec, now,
                            /*filter*/ (kr) -> isPigman(kr.type) && kr.cause == DamageCause.FALL)
                        >= goldThresh) {
                        schedulePublish(new DetectionResult(wid, r.x, r.y, r.z, goldRange, goldThresh));
                    }

                    // “Other farm” rule: neighbors of ANY type within other window/range
                    if (otherRange > 0 && otherSec > 0) {
                        if (countNearby(dq, r, otherRange, otherSec, now, /*no filter*/ null) >= otherThresh) {
                            schedulePublish(new DetectionResult(wid, r.x, r.y, r.z, otherRange, otherThresh));
                        }
                    }
                }
            }
        }
    }

    // Resolve the "pigman" type at runtime to be compatible with old/new APIs.
    // Newer APIs: ZOMBIFIED_PIGLIN; older: PIG_ZOMBIE. Avoid direct enum refs to compile everywhere.
    private static final EntityType ZP_TYPE;
    static {
        EntityType tmp = null;
        try {
            tmp = EntityType.valueOf("ZOMBIFIED_PIGLIN");
        } catch (IllegalArgumentException ignored) {}
        if (tmp == null) {
            try {
                tmp = EntityType.valueOf("PIG_ZOMBIE");
            } catch (IllegalArgumentException ignored2) {}
        }
        ZP_TYPE = tmp;
    }
    private static boolean isPigman(EntityType t) {
        return ZP_TYPE != null && t == ZP_TYPE;
    }

    private interface RecPredicate { boolean test(KillRecord kr); }

    private static int countNearby(Deque<KillRecord> dq, KillRecord center,
                                   double range, long windowSec, long now, RecPredicate pred) {
        final long cutoff = now - (windowSec * 1000L);
        final double r2 = range * range;
        int n = 0;

        for (Iterator<KillRecord> it = dq.descendingIterator(); it.hasNext(); ) {
            KillRecord k = it.next();
            if (k.timeMs < cutoff) break; // time-ordered → stop early
            if (k == center) continue;    // don’t count the same death
            if (pred != null && !pred.test(k)) continue;

            double dx = k.x - center.x;
            double dy = k.y - center.y;
            double dz = k.z - center.z;
            if ((dx*dx + dy*dy + dz*dz) <= r2) {
                n++;
            }
        }
        return n;
    }

    private void schedulePublish(DetectionResult res) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                publishOnMain.accept(res);
            } catch (Throwable t) {
                MessageHelper.debug("GrindingDetectionWorker publish failed: %s", t.getMessage());
            }
        });
    }
}

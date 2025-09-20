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

    // Worker-owned index. Accessed only on the async scheduler thread.
    private final Map<UUID, Deque<KillRecord>> byWorld = new ConcurrentHashMap<>();

    // Limits
    private static final int BATCH_LIMIT = 512;     // drain per pass
    private static final int MAX_DEQUE_SIZE = 8000; // per world soft cap

    // --- Debug metrics (cheap) ---
    private final LongAdder enqueued = new LongAdder();    // increments in offer()
    private final LongAdder drained  = new LongAdder();    // adds drained per pass
    private volatile long lastProcessNs;                   // duration of last process() run
    private volatile int  lastDrainedCount;                // how many records drained last run
    private volatile int  lastWorldsVisited;               // worlds scanned last run

    // Allow manager to clear a world's buffer on unload (safe due to CHM semantics)
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
        // Hot path stays O(1)
        queue.offer(r);
        enqueued.increment();
    }

    // Optional debug gauge (async). Call from manager if you want periodic logs.
    void startDebugGauge(long periodTicks) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            // Respect your plugin's debug gate—MessageHelper.debug() should no-op if disabled
            long backlog = enqueued.longValue() - drained.longValue();

            int worlds = byWorld.size();
            int sumBuf = 0, maxBuf = 0;
            for (Deque<KillRecord> dq : byWorld.values()) {
                int s = dq.size();
                sumBuf += s;
                if (s > maxBuf) maxBuf = s;
            }

            MessageHelper.debug(
                "GrindWorker: backlog=%d, drainedLast=%d, lastProc=%.3fms, worldsVisitedLast=%d, buffers(sum=%d, max=%d)",
                backlog, lastDrainedCount, (lastProcessNs / 1_000_000.0), lastWorldsVisited, sumBuf, maxBuf
            );
        }, periodTicks, periodTicks);
    }

    // ----------------------------------------------------------------
    // Async loop (runs off the main thread)
    // ----------------------------------------------------------------
    private void process() {
        final long t0 = System.nanoTime();

        // Drain a bounded batch each tick to avoid long async bursts
        int drainedThisPass = 0;
        for (int i = 0; i < BATCH_LIMIT; i++) {
            KillRecord r = queue.poll();
            if (r == null) break;
            drainedThisPass++;
            byWorld.computeIfAbsent(r.worldId, __ -> new ArrayDeque<>()).addLast(r);
        }
        if (drainedThisPass > 0) {
            drained.add(drainedThisPass);
        }
        lastDrainedCount = drainedThisPass;

        if (byWorld.isEmpty()) {
            lastWorldsVisited = 0;
            lastProcessNs = System.nanoTime() - t0;
            return;
        }

        final long now = System.currentTimeMillis();

        // Read config (no Bukkit calls)
        final var cfg = plugin.getConfigManager();

        // Windows/ranges
        final long enderSec     = cfg.secondsToSearchForGrindingOnEndermanFarms;
        final double enderRange = cfg.rangeToSearchForGrindingOnEndermanFarms;
        final int enderThresh   = cfg.numberOfDeathsWhenSearchingForGringdingOnEndermanFarms;

        final long goldSec      = cfg.secondsToSearchForGrinding;
        final double goldRange  = cfg.rangeToSearchForGrinding;
        final int goldThresh    = cfg.numberOfDeathsWhenSearchingForGringding;

        final long otherSec     = cfg.secondsToSearchForGrindingOnOtherFarms;
        final double otherRange = cfg.rangeToSearchForGrindingOnOtherFarms;
        final int otherThresh   = cfg.numberOfDeathsWhenSearchingForGringdingOnOtherFarms;

        // Precompute once per pass
        final long maxWindowSec = Math.max(Math.max(enderSec, goldSec), otherSec);
        final boolean enderEnabled = (enderRange > 0D && enderSec > 0L && enderThresh > 0);
        final boolean goldEnabled  = (goldRange  > 0D && goldSec  > 0L && goldThresh  > 0);
        final boolean otherEnabled = (otherRange > 0D && otherSec > 0L && otherThresh > 0);

        int worldsVisited = 0;

        // Process per world
        for (Map.Entry<UUID, Deque<KillRecord>> e : byWorld.entrySet()) {
            final UUID wid = e.getKey();
            final Deque<KillRecord> dq = e.getValue();
            if (dq.isEmpty()) continue;
            worldsVisited++;

            // Trim stale heads using the maximum window
            final long cutoff = now - (maxWindowSec * 1000L);
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
                if (enderEnabled && r.type == EntityType.ENDERMAN && r.cause == DamageCause.VOID) {
                    if (countNearby(dq, r, enderRange, enderSec, now,
                            /*filter*/ (kr) -> kr.type == EntityType.ENDERMAN && kr.cause == DamageCause.VOID,
                            enderThresh) >= enderThresh) {
                        schedulePublish(new DetectionResult(wid, r.x, r.y, r.z, enderRange, enderThresh));
                    }
                }

                // Nether Gold XP farm (ZombiePigman FALL)
                if (goldEnabled && isPigman(r.type) && r.cause == DamageCause.FALL) {
                    if (countNearby(dq, r, goldRange, goldSec, now,
                            /*filter*/ (kr) -> isPigman(kr.type) && kr.cause == DamageCause.FALL,
                            goldThresh) >= goldThresh) {
                        schedulePublish(new DetectionResult(wid, r.x, r.y, r.z, goldRange, goldThresh));
                    }

                    // “Other farm” rule: neighbors of ANY type within other window/range
                    if (otherEnabled) {
                        if (countNearby(dq, r, otherRange, otherSec, now, /*no filter*/ null, otherThresh) >= otherThresh) {
                            schedulePublish(new DetectionResult(wid, r.x, r.y, r.z, otherRange, otherThresh));
                        }
                    }
                }
            }
        }

        lastWorldsVisited = worldsVisited;
        lastProcessNs = System.nanoTime() - t0;
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
                                   double range, long windowSec, long now,
                                   RecPredicate pred, int threshold) {
        final long cutoff = now - (windowSec * 1000L);
        final double r2 = range * range;
        final double cx = center.x, cy = center.y, cz = center.z;
        int n = 0;

        for (Iterator<KillRecord> it = dq.descendingIterator(); it.hasNext(); ) {
            KillRecord k = it.next();
            if (k.timeMs < cutoff) break; // time-ordered → stop early
            if (k == center) continue;    // don’t count the same death
            if (pred != null && !pred.test(k)) continue;

            double dx = k.x - cx;
            double dy = k.y - cy;
            double dz = k.z - cz;
            if ((dx*dx + dy*dy + dz*dz) <= r2) {
                if (++n >= threshold) return n;  // EARLY EXIT
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


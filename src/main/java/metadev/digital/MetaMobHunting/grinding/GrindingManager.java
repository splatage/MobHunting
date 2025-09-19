package metadev.digital.MetaMobHunting.grinding;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.Collections; 
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import metadev.digital.metacustomitemslib.Tools;
import metadev.digital.metacustomitemslib.mobs.MobType;
import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.mobs.ExtendedMob;

public class GrindingManager implements Listener {

	private MobHunting plugin;
	
	// Per-world, time-ordered queue of recent kills (append on death, pop old in purge)
    private final Map<UUID, Deque<GrindingInformation>> killsByWorld = new ConcurrentHashMap<>();


	private boolean saveWhitelist = false;
	private boolean saveBlacklist = false;
	
	private static final Map<Integer, GrindingInformation> killed_mobs =
    	Collections.synchronizedMap(new LinkedHashMap<>());

	private static HashMap<UUID, LinkedList<Area>> mBlacklistedAreas = new HashMap<>();
	private static HashMap<UUID, LinkedList<Area>> mWhitelistedAreas = new HashMap<>();

	public GrindingManager(MobHunting instance) {
		this.plugin = instance;
		if (!loadWhitelist(instance))
			throw new RuntimeException();
		if (!loadBlacklist(instance))
			throw new RuntimeException();
		Bukkit.getPluginManager().registerEvents(this, instance);

		// Periodic purge to prevent unbounded growth when detectors aren't hit frequently.
		long windowSec = Math.max(
				Math.max(plugin.getConfigManager().secondsToSearchForGrindingOnOtherFarms,
						plugin.getConfigManager().secondsToSearchForGrindingOnEndermanFarms),
				plugin.getConfigManager().secondsToSearchForGrinding);
		windowSec = Math.max(windowSec, (long) plugin.getConfigManager().speedGrindingTimeFrame);
		// Run every 30s..120s based on window, minimum 30s.
		long purgeEveryTicks = 20L * Math.max(30, Math.min(120, (int) (windowSec / 2)));
		Bukkit.getScheduler().runTaskTimer(instance, this::purgeOldKills, purgeEveryTicks, purgeEveryTicks);
	}

	public void saveData() {
		if (saveWhitelist) {
			MessageHelper.debug("Saving whitelisted areas to disk.");
			saveWhitelist();
		}
		if (saveBlacklist) {
			MessageHelper.debug("Saving blacklisted areas to disk.");
			saveBlacklist();
		}
	}

	/**
	 * Register a kill for later inspection. Farming can be caught because most
	 * farms kills the mobs by letting them fall down from a high place.
	 *
	 * @param killed
	 */
public void registerDeath(LivingEntity killer, LivingEntity killed) {
	// Fast guards
	if (killed == null)
		return;

	final World world = killed.getWorld();
	if (world == null || isGrindingDisabledInWorld(world))
		return;

	final Location loc = killed.getLocation();
	if (loc == null || loc.getWorld() == null)
		return;

	// Only track if not in a known grinding or whitelisted area
	if (!isGrindingArea(loc) && !isWhitelisted(loc)) {
		GrindingInformation gi = new GrindingInformation(
				(killer != null ? killer.getUniqueId() : null),
				killed);
		// Existing map for legacy lookups & purge
		killed_mobs.put(killed.getEntityId(), gi);

		// NEW: per-world, time-ordered queue for fast same-world scans
		final UUID wuid = world.getUID();
        killsByWorld.computeIfAbsent(wuid, __ -> new ConcurrentLinkedDeque<>()).addLast(gi);
	}
}



	/**
	 * Test if the same type of mobs is killed to fast. When players make a farm,
	 * they tend to kill the mobs to fast.
	 *
	 * @param killed
	 * @return
	 */
    public boolean isPlayerSpeedGrinding(LivingEntity killer, LivingEntity killed) {
        long starttime = System.currentTimeMillis();
        int n = 0;
        long oldestKill = starttime;

        ExtendedMob mob = plugin.getExtendedMobManager().getExtendedMobFromEntity(killed);

        synchronized (killed_mobs) {
            Iterator<Entry<Integer, GrindingInformation>> itr = killed_mobs.entrySet().iterator();
            while (itr.hasNext()) {
                GrindingInformation gi = itr.next().getValue();
    
                if (killer == null) {
                    return false;
                }
                if (gi == null || gi.getKillerUUID() == null) {
                    continue;
                }

                // Purge entries whose entity has already been GC'ed (weak-ref cleared).
                if (gi.getKilled() == null) {
                    itr.remove();
                    continue;
                }

                if (!killer.getUniqueId().equals(gi.getKillerUUID()))
                    continue;

                if (starttime > gi.getTimeOfDeath() + (1000L * plugin.getConfigManager().speedGrindingTimeFrame)) {
                    itr.remove();
                    continue;
                }

                n++; // No of killed mobs
                oldestKill = Math.min(oldestKill, gi.getTimeOfDeath());
            }
        }

        if (n == 0) {
            return false;
        }

        long timeframe = (starttime - oldestKill) / 1000L;
        long avg_time = timeframe / (long) n; // sec.

        MessageHelper.debug(
            "%s has killed %s %s in %s seconds. Avg.kill time %s must greater than %s when %s mobs is killed.",
            killer.getName(), n, mob.getMobName(), timeframe, avg_time,
            plugin.getConfigManager().speedGrindingTimeFrame / plugin.getConfigManager().speedGrindingNoOfMobs,
            plugin.getConfigManager().speedGrindingNoOfMobs
        );

        if (avg_time != 0
                && n >= plugin.getConfigManager().speedGrindingNoOfMobs
                && avg_time < plugin.getConfigManager().speedGrindingTimeFrame
                        / plugin.getConfigManager().speedGrindingNoOfMobs) {
            return true;
        } else {
            return false;
        }
    }



	/**
	 * Test if the killed mob is killed in a NetherGoldXPFarm
	 *
	 * @param killed
	 * @param silent
	 * @return true if the location is detected as a NetherGoldXPFarm, or if the
	 *         area is detected as a Grinding Area
	 */
public boolean isNetherGoldXPFarm(LivingEntity killed, boolean silent) {
	int n = 0;
	long now = System.currentTimeMillis();
	final long seconds = plugin.getConfigManager().secondsToSearchForGrinding;
	final double killRadius = plugin.getConfigManager().rangeToSearchForGrinding;
	final int numberOfDeaths = plugin.getConfigManager().numberOfDeathsWhenSearchingForGringding;

	if (killed == null) return false;

	if (MobType.getMobType(killed) == MobType.ZombiePigman) {
		if (killed.getLastDamageCause() == null) return false;
		if (killed.getLastDamageCause().getCause() == DamageCause.FALL) {

			// Quick guard for misconfigured radius.
			if (killRadius <= 0D) return false;

			final World world = killed.getWorld();
			if (world == null) return false;

			final Location killedLoc = killed.getLocation();
			final double radiusSq = killRadius * killRadius;
			final int killedId = killed.getEntityId();
			final long cutoff = now - (seconds * 1000L);

			Area detectedGrindingArea = getGrindingArea(killedLoc);
			if (detectedGrindingArea != null) {
				if (!silent) {
					World w = detectedGrindingArea.getCenter().getWorld();
					MessageHelper.debug("This is a known grinding area: (%s,%s,%s,%s)",
							(w == null ? "<unloaded>" : w.getName()),
							detectedGrindingArea.getCenter().getBlockX(),
							detectedGrindingArea.getCenter().getBlockY(),
							detectedGrindingArea.getCenter().getBlockZ());
				}
				return true;
			}

			// Iterate only recent kills in THIS world, newest->oldest; break at cutoff
			final Deque<GrindingInformation> dq = killsByWorld.get(world.getUID());
			if (dq != null) {
				// Opportunistic trimming of stale heads (keeps scans short)
				for (;;) {
					GrindingInformation head = dq.peekFirst();
					if (head == null) break;
					if (head.getTimeOfDeath() < cutoff || head.getKilled() == null) {
						dq.pollFirst();
					} else {
						break;
					}
				}
				for (Iterator<GrindingInformation> it = dq.descendingIterator(); it.hasNext();) {
					GrindingInformation gi = it.next();
					if (gi == null) continue;

					// Stop scanning once we’re older than the window
					if (gi.getTimeOfDeath() < cutoff) break;

					Entity e = gi.getKilled();
					if (e == null) continue; // let purge remove later
					if (e.getEntityId() == killedId) continue;

					// Match Pigmen (keeps your MobType logic)
					if (e instanceof LivingEntity && MobType.getMobType((LivingEntity) e) == MobType.ZombiePigman) {
						Location eLoc = e.getLocation();
						if (eLoc != null && killedLoc.distanceSquared(eLoc) <= radiusSq) {
							n++;
							if (n >= numberOfDeaths) {
								Area area = new Area(killedLoc, killRadius, numberOfDeaths);
								MessageHelper.debug("New Nether Gold XP Farm detected at (%s,%s,%s,%s)",
										area.getCenter().getWorld().getName(), area.getCenter().getBlockX(),
										area.getCenter().getBlockY(), area.getCenter().getBlockZ());
								registerKnownGrindingSpot(area);
								return true;
							}
						}
					}
				}
			}
		}
	}
	if (!silent)
		MessageHelper.debug(
				"Farm detection: This was not a Nether Gold XP Farm (%s of %s mobs with last %s sec.)", n,
				numberOfDeaths, seconds);
	return false;
}



	/**
	 * Test if the killed mob is killed in a EndermanFarm
	 *
	 * @param killed
	 * @param silent
	 * @return true if the locatnewAreaion is detected as a EndermanFarm, or if the
	 *         area is detected as a Grinding Area
	 */
public boolean isEndermanFarm(LivingEntity killed, boolean silent) {
	int n = 0;
	long now = System.currentTimeMillis();
	final long seconds = plugin.getConfigManager().secondsToSearchForGrindingOnEndermanFarms;
	final double killRadius = plugin.getConfigManager().rangeToSearchForGrindingOnEndermanFarms;
	final int numberOfDeaths = plugin.getConfigManager().numberOfDeathsWhenSearchingForGringdingOnEndermanFarms;

	if (killed == null) return false;

	if (MobType.getMobType(killed) == MobType.Enderman) {
		if (killed.getLastDamageCause() == null) return false;
		if (killed.getLastDamageCause().getCause() == DamageCause.VOID) {

			// Guard against misconfigured radius.
			if (killRadius <= 0D) return false;

			final World world = killed.getWorld();
			if (world == null) return false;

			final Location killedLoc = killed.getLocation();
			final double radiusSq = killRadius * killRadius;
			final int killedId = killed.getEntityId();
			final long cutoff = now - (seconds * 1000L);

			Area detectedGrindingArea = getGrindingArea(killedLoc);
			if (detectedGrindingArea != null) {
				if (!silent) {
					World w = detectedGrindingArea.getCenter().getWorld();
					MessageHelper.debug("This is a known Enderman Farm area: (%s,%s,%s,%s)",
							(w == null ? "<unloaded>" : w.getName()),
							detectedGrindingArea.getCenter().getBlockX(),
							detectedGrindingArea.getCenter().getBlockY(),
							detectedGrindingArea.getCenter().getBlockZ());
				}
				return true;
			}

			final Deque<GrindingInformation> dq = killsByWorld.get(world.getUID());
			if (dq != null) {
				// Opportunistic trimming of stale heads
				for (;;) {
					GrindingInformation head = dq.peekFirst();
					if (head == null) break;
					if (head.getTimeOfDeath() < cutoff || head.getKilled() == null) {
						dq.pollFirst();
					} else {
						break;
					}
				}
				for (Iterator<GrindingInformation> it = dq.descendingIterator(); it.hasNext();) {
					GrindingInformation gi = it.next();
					if (gi == null) continue;
					if (gi.getTimeOfDeath() < cutoff) break;

					Entity e = gi.getKilled();
					if (e == null) continue;
					if (e.getEntityId() == killedId) continue;

					if (e.getType() == EntityType.ENDERMAN) {
						Location eLoc = e.getLocation();
						if (eLoc != null && killedLoc.distanceSquared(eLoc) <= radiusSq) {
							n++;
							if (n >= numberOfDeaths) {
								Area area = new Area(killedLoc, killRadius, numberOfDeaths);
								MessageHelper.debug("New Enderman Farm detected at (%s,%s,%s,%s)",
										area.getCenter().getWorld().getName(), area.getCenter().getBlockX(),
										area.getCenter().getBlockY(), area.getCenter().getBlockZ());
								registerKnownGrindingSpot(area);
								return true;
							}
						}
					}
				}
			}
		}
	}

	if (!silent)
		MessageHelper.debug(
				"Farm detection: This was not an Enderman Farm (%s of %s mobs with last %s sec.) at (%s,%s,%s,%s)",
				n, numberOfDeaths, seconds, killed.getWorld(), killed.getLocation().getX(),
				killed.getLocation().getY(), killed.getLocation().getZ());
	return false;
}





public boolean isOtherFarm(LivingEntity killed, boolean silent) {
	int n = 0;
	long now = System.currentTimeMillis();
	final long seconds = plugin.getConfigManager().secondsToSearchForGrindingOnOtherFarms;
	final double killRadius = plugin.getConfigManager().rangeToSearchForGrindingOnOtherFarms;
	final int numberOfDeaths = plugin.getConfigManager().numberOfDeathsWhenSearchingForGringdingOnOtherFarms;

	if (killed == null) return false;

	if (MobType.getMobType(killed) == MobType.ZombiePigman) {
		if (killed.getLastDamageCause() == null) return false;
		if (killed.getLastDamageCause().getCause() == DamageCause.FALL) {

			// Guard against misconfiguration
			if (killRadius <= 0D) return false;

			final World killedWorld = killed.getWorld();
			if (killedWorld == null) return false;

			final Location killedLoc = killed.getLocation();
			final int killedId = killed.getEntityId();
			final double radiusSq = killRadius * killRadius;
			final long cutoff = now - (seconds * 1000L);

			Area detectedGrindingArea = getGrindingArea(killedLoc);
			if (detectedGrindingArea != null) {
				if (!silent)
					MessageHelper.debug("This is a known grinding area: (%s,%s,%s,%s)",
							detectedGrindingArea.getCenter().getWorld() != null
									? detectedGrindingArea.getCenter().getWorld().getName()
									: "<unloaded>",
							detectedGrindingArea.getCenter().getBlockX(),
							detectedGrindingArea.getCenter().getBlockY(),
							detectedGrindingArea.getCenter().getBlockZ());
				return true;
			}

			final Deque<GrindingInformation> dq = killsByWorld.get(killedWorld.getUID());
			if (dq != null) {
				// Opportunistic trimming of stale heads
				for (;;) {
					GrindingInformation head = dq.peekFirst();
					if (head == null) break;
					if (head.getTimeOfDeath() < cutoff || head.getKilled() == null) {
						dq.pollFirst();
					} else {
						break;
					}
				}
				for (Iterator<GrindingInformation> it = dq.descendingIterator(); it.hasNext();) {
					GrindingInformation gi = it.next();
					if (gi == null) continue;
					if (gi.getTimeOfDeath() < cutoff) break;

					Entity e = gi.getKilled();
					if (e == null) continue;
					if (e.getEntityId() == killedId) continue;

					// NOTE: "other farm" logic in your original code did not filter type,
					// so we keep it that way: any nearby mob death within the window counts.
					Location eLoc = e.getLocation();
					if (eLoc != null && killedLoc.distanceSquared(eLoc) <= radiusSq) {
						n++;
						if (n >= numberOfDeaths) {
							Area area = new Area(killedLoc, killRadius, numberOfDeaths);
							if (!silent)
								MessageHelper.debug("New Generic / Other Farm detected at (%s,%s,%s,%s)",
										area.getCenter().getWorld().getName(), area.getCenter().getBlockX(),
										area.getCenter().getBlockY(), area.getCenter().getBlockZ());
							registerKnownGrindingSpot(area);
							return true;
						}
					}
				}
			}
		}
	}

	if (!silent)
		MessageHelper.debug(
				"Farm detection: This was not a generic / other Farm (%s of %s mobs with last %s sec.) at (%s,%s,%s,%s)",
				n, numberOfDeaths, seconds, killed.getWorld(), killed.getLocation().getX(),
				killed.getLocation().getY(), killed.getLocation().getZ());
	return false;
}





	// ****************************************************************
	// Events
	// ****************************************************************
	@EventHandler
	private void onWorldLoad(WorldLoadEvent event) {
		List<Area> areas = getWhitelistedAreas(event.getWorld());
		if (areas != null) {
			for (Area area : areas)
				area.getCenter().setWorld(event.getWorld());
		}
	}


    @EventHandler
    private void onWorldUnLoad(WorldUnloadEvent event) {
	    List<Area> areas = getWhitelistedAreas(event.getWorld());
	    if (areas != null) {
	    	for (Area area : areas)
	    		area.getCenter().setWorld(null);
	    }
	    killsByWorld.remove(event.getWorld().getUID()); // optional cleanup
    }

	// ****************************************************************
	// Blacklist
	// ****************************************************************
	public LinkedList<Area> getKnownGrindingSpots(Location loc) {
		if (loc == null) return new LinkedList<>();
		World w = loc.getWorld();
		if (w == null) return new LinkedList<>();

		if (mBlacklistedAreas.containsKey(w.getUID()))
			return mBlacklistedAreas.get(w.getUID());
		else
			return new LinkedList<Area>();
	}


	public void addKnownGrindingSpot(Area area) {
		if (area == null) return;
		Location c = area.getCenter();
		if (c == null) return;
		World w = c.getWorld();
		if (w == null) return;

		LinkedList<Area> list = getKnownGrindingSpots(c);
		list.add(area);
		mBlacklistedAreas.put(w.getUID(), list);
		saveBlacklist = true;
	}


	private boolean saveBlacklist() {
		YamlConfiguration blacklist = new YamlConfiguration();
		File file = new File(MobHunting.getInstance().getDataFolder(), "blacklist.yml");

		try {
			for (Entry<UUID, LinkedList<Area>> entry : mBlacklistedAreas.entrySet()) {
				ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
				if (entry.getValue() == null) continue;

				for (Area area : entry.getValue()) {
					if (area == null || area.getCenter() == null) continue;

					HashMap<String, Object> map = new HashMap<String, Object>();
					map.put("Center", Tools.toMap(area.getCenter()));
					map.put("Radius", area.getRange());
					map.put("Counter", area.getCounter());
					map.put("Time", area.getTime());
					list.add(map);
				}
				blacklist.set(entry.getKey().toString(), list);
			}

			blacklist.save(file);
			saveBlacklist = false;
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			// Defensive: catch any unexpected serialization issue
			e.printStackTrace();
			return false;
		}
	}


	@SuppressWarnings("unchecked")
	private boolean loadBlacklist(MobHunting instance) {
		YamlConfiguration blacklist = new YamlConfiguration();
		File file = new File(instance.getDataFolder(), "blacklist.yml");

		if (!file.exists())
			return true;

		try {
			blacklist.load(file);
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
			return false;
		}

		mBlacklistedAreas.clear();

		try {
			for (String worldId : blacklist.getKeys(false)) {
				UUID worldUuid;
				try {
					worldUuid = UUID.fromString(worldId);
				} catch (IllegalArgumentException ex) {
					// Skip malformed key
					continue;
				}

				List<?> rawList = blacklist.getList(worldId);
				if (rawList == null) continue;

				LinkedList<Area> areas = new LinkedList<Area>();
				for (Object o : rawList) {
					if (!(o instanceof Map)) continue;
					Map<String, Object> map = (Map<String, Object>) o;

					try {
						Object centerObj = map.get("Center");
						if (!(centerObj instanceof Map)) continue;
						Location center = Tools.fromMap((Map<String, Object>) centerObj);
						if (center == null) continue;

						Object r = map.get("Radius");
						double radius = (r instanceof Number) ? ((Number) r).doubleValue() : 0D;

						Object c = map.get("Counter");
						int counter = (c instanceof Number) ? ((Number) c).intValue() : 0;

						Object t = map.get("Time");
						long time = (t instanceof Number) ? ((Number) t).longValue() : System.currentTimeMillis();

						areas.add(new Area(center, radius, counter, time));
					} catch (ClassCastException ex) {
						// Skip malformed entry, continue loading others
						continue;
					}
				}

				// Only insert for currently loaded worlds (matches original behavior)
				for (World w : Bukkit.getWorlds()) {
					if (w.getUID().equals(worldUuid)) {
						mBlacklistedAreas.put(worldUuid, areas);
						break;
					}
				}
			}
		} catch (Exception e) {
			// Defensive: avoid failing the whole load on a single bad node
			e.printStackTrace();
			// still return true so plugin can continue; file might be partially usable
		}

		return true;
	}

	public void registerKnownGrindingSpot(Area newArea) {
		for (Area area : getKnownGrindingSpots(newArea.getCenter())) {
			if (newArea.getCenter().getWorld().equals(area.getCenter().getWorld())) {
				double dist = newArea.getCenter().distance(area.getCenter());

				double remaining = dist;
				remaining -= area.getRange();
				remaining -= newArea.getRange();

				if (remaining < 0) {
					if (dist > area.getRange())
						area.setRange(dist);

					area.setCounter(newArea.getCounter() + 1);

					return;
				}
			}
		}

		addKnownGrindingSpot(newArea);
	}

	public Area getGrindingArea(Location location) {
		if (location == null) return null;
		final World world = location.getWorld();
		if (world == null) return null;

		LinkedList<Area> areas = getKnownGrindingSpots(location);
		final double lx = location.getX();
		final double ly = location.getY();
		final double lz = location.getZ();

		for (Area area : areas) {
			final Location c = area.getCenter();
			final World cw = c.getWorld();
			if (cw == null || !cw.equals(world)) continue;

			final double dx = c.getX() - lx;
			final double dy = c.getY() - ly;
			final double dz = c.getZ() - lz;
			if ((dx * dx + dy * dy + dz * dz) < (area.getRange() * area.getRange())) {
				return area;
			}
		}
		return null;
	}



	public boolean isGrindingArea(Location location) {
		if (location == null) return false;
		final World world = location.getWorld();
		if (world == null) return false;

		LinkedList<Area> areas = getKnownGrindingSpots(location);
		final double lx = location.getX();
		final double ly = location.getY();
		final double lz = location.getZ();

		for (Area area : areas) {
			final Location c = area.getCenter();
			final World cw = c.getWorld();
			if (cw == null || !cw.equals(world)) continue;

			final double dx = c.getX() - lx;
			final double dy = c.getY() - ly;
			final double dz = c.getZ() - lz;
			if ((dx * dx + dy * dy + dz * dz) < (area.getRange() * area.getRange())) {
				return true;
			}
		}
		return false;
	}



	public void clearGrindingArea(Location location) {
		if (location == null) return;
		final World world = location.getWorld();
		if (world == null) return;

		final double lx = location.getX();
		final double ly = location.getY();
		final double lz = location.getZ();

		Iterator<Area> it = getKnownGrindingSpots(location).iterator();
		while (it.hasNext()) {
			Area area = it.next();
			final Location c = area.getCenter();
			final World cw = c.getWorld();

			// If the area's world is null (unloaded) or matches the location's world, consider removal.
			if (cw == null || cw.equals(world)) {
				final double dx = c.getX() - lx;
				final double dy = c.getY() - ly;
				final double dz = c.getZ() - lz;
				if ((dx * dx + dy * dy + dz * dz) < (area.getRange() * area.getRange())) {
					it.remove();
				}
			}
		}
	}



	public void blacklistArea(Area newArea) {
		if (newArea == null) return;
		Location c = newArea.getCenter();
		if (c == null) return;
		World w = c.getWorld();
		if (w == null) return;

		LinkedList<Area> areas = mBlacklistedAreas.get(w.getUID());
		if (areas == null) {
			areas = new LinkedList<Area>();
		}

		for (Area area : areas) {
			if (c.getWorld().equals(area.getCenter().getWorld())) {
				double dist = c.distance(area.getCenter());

				double remaining = dist;
				remaining -= area.getRange();
				remaining -= newArea.getRange();

				if (remaining < 0) {
					if (dist > area.getRange())
						area.setRange(dist);

					area.setCounter(newArea.getCounter() + 1);
					mBlacklistedAreas.put(w.getUID(), areas);
					saveBlacklist = true;
					return;
				}
			}
		}
		areas.add(newArea);
		mBlacklistedAreas.put(w.getUID(), areas);
		saveBlacklist = true;
	}


	public void unBlacklistArea(Location location) {
		if (location == null) return;
		World w = location.getWorld();
		if (w == null) return;

		LinkedList<Area> areas = mBlacklistedAreas.get(w.getUID());
		if (areas == null) return;

		Iterator<Area> it = areas.iterator();
		while (it.hasNext()) {
			Area area = it.next();
			Location c = area.getCenter();
			if (c == null) continue;
			World cw = c.getWorld();
			if (cw != null && !cw.equals(w)) continue;

			if (c.distance(location) < area.getRange()) {
				it.remove();
			}
		}
		if (areas.isEmpty())
			mBlacklistedAreas.remove(w.getUID());
		else
			mBlacklistedAreas.put(w.getUID(), areas);
		saveBlacklist = true;
	}


	// ****************************************************************
	// Whitelisted Areas
	// ****************************************************************

	public LinkedList<Area> getWhitelistedAreas(World world) {
		if (world == null) return new LinkedList<>();
		if (mWhitelistedAreas.containsKey(world.getUID()))
			return mWhitelistedAreas.get(world.getUID());
		else
			return new LinkedList<Area>();
	}


	private boolean saveWhitelist() {
		YamlConfiguration whitelist = new YamlConfiguration();
		File file = new File(MobHunting.getInstance().getDataFolder(), "whitelist.yml");

		try {
			for (Entry<UUID, LinkedList<Area>> entry : mWhitelistedAreas.entrySet()) {
				ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
				if (entry.getValue() == null) continue;

				for (Area area : entry.getValue()) {
					if (area == null || area.getCenter() == null) continue;

					HashMap<String, Object> map = new HashMap<String, Object>();
					map.put("Center", Tools.toMap(area.getCenter()));
					map.put("Radius", area.getRange());
					map.put("Counter", area.getCounter());
					map.put("Time", area.getTime());
					list.add(map);
				}
				whitelist.set(entry.getKey().toString(), list);
			}

			whitelist.save(file);
			saveWhitelist = false;
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			// Defensive: catch any unexpected serialization issue
			e.printStackTrace();
			return false;
		}
	}


	@SuppressWarnings("unchecked")
	private boolean loadWhitelist(MobHunting instance) {
		YamlConfiguration whitelist = new YamlConfiguration();
		File file = new File(instance.getDataFolder(), "whitelist.yml");

		if (!file.exists())
			return true;

		try {
			whitelist.load(file);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
			return false;
		}

		mWhitelistedAreas.clear();

		try {
			for (String worldId : whitelist.getKeys(false)) {
				UUID worldUuid;
				try {
					worldUuid = UUID.fromString(worldId);
				} catch (IllegalArgumentException ex) {
					// Skip malformed key
					continue;
				}

				List<?> rawList = whitelist.getList(worldId);
				if (rawList == null) continue;

				LinkedList<Area> areas = new LinkedList<Area>();
				for (Object o : rawList) {
					if (!(o instanceof Map)) continue;
					Map<String, Object> map = (Map<String, Object>) o;

					try {
						Object centerObj = map.get("Center");
						if (!(centerObj instanceof Map)) continue;
						Location center = Tools.fromMap((Map<String, Object>) centerObj);
						if (center == null) continue;

						Object r = map.get("Radius");
						double radius = (r instanceof Number) ? ((Number) r).doubleValue() : 0D;

						Object c = map.get("Counter");
						int counter = (c instanceof Number) ? ((Number) c).intValue() : 0;

						Object t = map.get("Time");
						long time = (t instanceof Number) ? ((Number) t).longValue() : System.currentTimeMillis();

						areas.add(new Area(center, radius, counter, time));
					} catch (ClassCastException ex) {
						// Skip malformed entry, continue with others
						continue;
					}
				}

				// Only insert for currently loaded worlds (preserves original behavior)
				for (World w : Bukkit.getWorlds()) {
					if (w.getUID().equals(worldUuid)) {
						mWhitelistedAreas.put(worldUuid, areas);
						break;
					}
				}
			}
		} catch (Exception e) {
			// Defensive: avoid failing the whole load on a single bad node
			e.printStackTrace();
			// still return true; partial lists are better than none
		}

		return true;
	}


	public boolean isWhitelisted(Location location) {
		if (location == null) return false;
		World w = location.getWorld();
		if (w == null) return false;

		LinkedList<Area> areas = mWhitelistedAreas.get(w.getUID());
		if (areas == null) return false;

		for (Area area : areas) {
			Location c = area.getCenter();
			if (c == null || c.getWorld() == null || !c.getWorld().equals(w)) continue;
			if (c.distance(location) < area.getRange()) {
				return true;
			}
		}
		return false;
	}



	public Area getWhitelistArea(Location location) {
		if (location == null) return null;
		final World world = location.getWorld();
		if (world == null) return null;

		LinkedList<Area> areas = getWhitelistedAreas(world);
		final double lx = location.getX();
		final double ly = location.getY();
		final double lz = location.getZ();

		for (Area area : areas) {
			final Location c = area.getCenter();
			final World cw = c.getWorld();
			if (cw == null || !cw.equals(world)) continue;

			final double dx = c.getX() - lx;
			final double dy = c.getY() - ly;
			final double dz = c.getZ() - lz;
			if ((dx * dx + dy * dy + dz * dz) < (area.getRange() * area.getRange())) {
				return area;
			}
		}
		return null;
	}


	public void whitelistArea(Area newArea) {
		if (newArea == null) return;
		Location c = newArea.getCenter();
		if (c == null) return;
		World w = c.getWorld();
		if (w == null) return;

		LinkedList<Area> areas = mWhitelistedAreas.get(w.getUID());
		if (areas == null) {
			areas = new LinkedList<Area>();
			mWhitelistedAreas.put(w.getUID(), areas);
		}

		for (Area area : areas) {
			if (c.getWorld().equals(area.getCenter().getWorld())) {
				double dist = c.distance(area.getCenter());

				double remaining = dist;
				remaining -= area.getRange();
				remaining -= newArea.getRange();

				if (remaining < 0) {
					if (dist > area.getRange())
						area.setRange(dist);

					area.setCounter(newArea.getCounter() + 1);
					saveWhitelist = true;
					return;
				}
			}
		}
		areas.add(newArea);
		mWhitelistedAreas.put(w.getUID(), areas);
		saveWhitelist = true;
	}


	public void unWhitelistArea(Location location) {
		if (location == null) return;
		World w = location.getWorld();
		if (w == null) return;

		LinkedList<Area> areas = mWhitelistedAreas.get(w.getUID());
		if (areas == null) return;

		Iterator<Area> it = areas.iterator();
		while (it.hasNext()) {
			Area area = it.next();
			Location c = area.getCenter();
			if (c == null) continue;
			World cw = c.getWorld();
			if (cw != null && !cw.equals(w)) continue;

			if (c.distance(location) < area.getRange())
				it.remove();
		}
		if (areas.isEmpty())
			mWhitelistedAreas.remove(w.getUID());
		else
			mWhitelistedAreas.put(w.getUID(), areas);
		saveWhitelist = true;
	}


	/**
	 * Check if Grindind detection is disabled in world
	 *
	 * @param world
	 * @return true if Grinding is disabled.
	 */
	public boolean isGrindingDisabledInWorld(World world) {
		if (world != null)
			for (String worldName : plugin.getConfigManager().disableGrindingDetectionInWorlds) {
				if (world.getName().equalsIgnoreCase(worldName))
					return true;
			}
		return false;
	}

	public void showGrindingArea(Player player, Area grindingArea, Location killedLocation) {

		// Fast guard: no player or not online.
		if (player == null || !player.isOnline())
			return;

		final World playerWorld = player.getWorld();

		// Smoke column at killed location (only if same world as the viewer)
		if (killedLocation != null && killedLocation.getWorld() == playerWorld) {
			final double kx = killedLocation.getX();
			final double kz = killedLocation.getZ();
			final double kBy = killedLocation.getBlockY(); // int widened to double

			for (int n = 0; n < 5; n++) {
				double y = kBy + 0.2 + 0.4 * n;
				player.spawnParticle(Particle.SMOKE, kx, y, kz, 5);
			}
		}

		// Grinding Area visuals (only if area is present and in the same world as the viewer)
		if (grindingArea != null) {
			final Location center = grindingArea.getCenter();
			final World centerWorld = center.getWorld();
			if (centerWorld == null || centerWorld != playerWorld)
				return;

			final double cx = center.getX();
			final double cz = center.getZ();
			final double cBy = center.getBlockY();

			// Show center of grinding area (hearts up the column)
			for (int n = 0; n < 5; n++) {
				double y = cBy + 0.2 + 0.4 * n;
				player.spawnParticle(Particle.HEART, cx, y, cz, 1);
			}

			// Circle around the grinding area
			double range = plugin.getConfigManager().grindingDetectionRange;
			if (range <= 0)
				return; // guard against bad config

			// Ensure a positive angular step (degrees), proportional to range like before
			int stepDeg = Math.max(1, (int) (45 / range));

			final double baseX = center.getBlockX() + 0.5;
			final double baseZ = center.getBlockZ() + 0.5;
			final double y = cBy + 0.2;

			for (int deg = 0; deg < 360; deg += stepDeg) {
				double rad = Math.toRadians(deg); // Java trig uses radians
				double x = baseX + Math.cos(rad) * range;
				double z = baseZ + Math.sin(rad) * range;
				player.spawnParticle(Particle.HEART, x, y, z, 1);
			}
		}
	}


	// ---------------------------------------------------------------------
	// Internal maintenance
	// ---------------------------------------------------------------------
    private void purgeOldKills() {
    	long now = System.currentTimeMillis();
    	long windowSec = Math.max(
    			Math.max(plugin.getConfigManager().secondsToSearchForGrindingOnOtherFarms,
    					plugin.getConfigManager().secondsToSearchForGrindingOnEndermanFarms),
    			plugin.getConfigManager().secondsToSearchForGrinding);
    	windowSec = Math.max(windowSec, (long) plugin.getConfigManager().speedGrindingTimeFrame);
    	long cutoff = now - (windowSec * 1000L);


    // Original map purge (must synchronize because purge runs async)
    synchronized (killed_mobs) {
        Iterator<Entry<Integer, GrindingInformation>> itr = killed_mobs.entrySet().iterator();
        while (itr.hasNext()) {
            GrindingInformation gi = itr.next().getValue();
            if (gi == null) {
                itr.remove();
                continue;
            }
            // Remove if outside time window or if the weak-referenced entity is gone.
            if (gi.getTimeOfDeath() < cutoff || gi.getKilled() == null) {
                itr.remove();
            }
        }
    }

	    // NEW: trim per-world queues from the head while entries are stale
    	for (Deque<GrindingInformation> dq : killsByWorld.values()) {
    		while (!dq.isEmpty()) {
    			GrindingInformation head = dq.peekFirst();
    			if (head == null || head.getTimeOfDeath() < cutoff || head.getKilled() == null) {
    				dq.pollFirst(); // drop stale
    			} else {
    				break; // rest are newer
    			}
    		}
    	}
    }
}


package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import me.lokka30.levelledmobs.LevelInterface;
import me.lokka30.levelledmobs.LevelledMobs;
import one.lindegaard.Core.compatibility.CompatPlugin;
import one.lindegaard.MobHunting.MobHunting;

public class LevelledMobsCompat implements Listener {

	// https://www.spigotmc.org/resources/levelledmobs-for-1-14-x-1-17-x.74304/

	private static boolean supported = false;
	private static Plugin mPlugin;

	public LevelledMobsCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getLogger().info(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Compatibility with LevelledMobs is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(CompatPlugin.LevelledMobs.getName());

			if (mPlugin.getDescription().getVersion().compareTo("3.0.6") >= 0) {
				Bukkit.getPluginManager().registerEvents(this, MobHunting.getInstance());
				Bukkit.getConsoleSender()
						.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
								+ "Enabling Compatibility with LevelledMobs ("
								+ getLevelledMobs().getDescription().getVersion() + ")");
				supported = true;
			} else {
				Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RED
						+ "Your current version of LevelledMobs (" + mPlugin.getDescription().getVersion()
						+ ") is not supported by MobHunting. Please update LevelledMobs to version 3.0.6 or newer.");
			}
		}

	}

	// **************************************************************************
	// OTHER FUNCTIONS
	// **************************************************************************
	public static Plugin getLevelledMobs() {
		return mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	private static final LevelInterface getLevelInterface() {
		return ((LevelledMobs) getLevelledMobs()).levelInterface;
	}

	public static boolean isLevelledMobs(Entity entity) {
		if (!isSupported())
			return false;
		if (!(entity instanceof LivingEntity))
			return false;

		return getLevelInterface().isLevelled((LivingEntity) entity);
	}

	public static Integer getLevelledMobsLevel(Entity entity) {
		if (!isSupported())
			return 1;

		return getLevelInterface().getLevelOfMob((LivingEntity) entity);
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationLevelledMobs;
	}

	// **************************************************************************
	// EVENTS
	// **************************************************************************

	/**@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void LevelledMobsSpawnEvent(EntitySpawnEvent event) {
		Entity entity = event.getEntity();
		if (isLevelledMobs(entity)) {
			int level = getMobLevel(entity);
			// Core.getMessages().debug("LevelledMobsSpawnEvent: MinecraftMobtype=%s
			// Level=%s", entity.getType(), level);
		}
	}**/

}

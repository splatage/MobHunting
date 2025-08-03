package metadev.digital.MetaMobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import io.github.arcaneplugins.levelledmobs.LevelInterface2;
import io.github.arcaneplugins.levelledmobs.LevelledMobs;

import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.MobHunting;

public class LevelledMobsCompat {

	// https://www.spigotmc.org/resources/levelledmobs-for-1-14-x-1-17-x.74304/

	private static boolean supported = false;
	private static LevelledMobs levelledMobs;
	private final String latestSupported = "4.3.1";

	public LevelledMobsCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender().sendMessage(
					MobHunting.PREFIX_WARNING + "Compatibility with LevelledMobs is disabled in config.yml");
		} else {
			levelledMobs = (LevelledMobs) Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.LevelledMobs.getName());

			if(levelledMobs == null)
				return;

            if ( levelledMobs.getDescription().getVersion().compareTo(latestSupported) >= 0) {
				Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX + "Enabling Compatibility with LevelledMobs ("
						+ getLevelledMobs().getDescription().getVersion() + ")");
				supported = true;
			} else {
				Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX_WARNING
						+ "Your current version of LevelledMobs (" + levelledMobs.getDescription().getVersion()
						+ ") is not supported by MobHunting. Please upgrade to " + latestSupported + " or newer.");
			}
		}

	}

	// **************************************************************************
	// OTHER FUNCTIONS
	// **************************************************************************
	public static LevelledMobs getLevelledMobs() {
		return levelledMobs;
	}

	public static boolean isSupported() {
		if(levelledMobs != null && levelledMobs.isEnabled())
			return supported;
		else return false;
    }

	private static LevelInterface2 getLevelInterface() {
		return getLevelledMobs().getLevelInterface();
	}

	public static boolean isLevelledMobs(Entity entity) {
		if (!supported)
			return false;
		if (!(entity instanceof LivingEntity))
			return false;

		return getLevelInterface().isLevelled((LivingEntity) entity);
	}

	public static Integer getLevelledMobsLevel(Entity entity) {
		if (!supported)
			return 1;

		return getLevelInterface().getLevelOfMob((LivingEntity) entity);
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationLevelledMobs;
	}

}

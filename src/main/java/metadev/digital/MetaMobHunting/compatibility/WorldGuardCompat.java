package metadev.digital.MetaMobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;

import metadev.digital.metacustomitemslib.server.Servers;
import metadev.digital.metacustomitemslib.compatibility.CompatPlugin;
import metadev.digital.MetaMobHunting.MobHunting;

public class WorldGuardCompat {

	private static boolean supported = false;
	private static WorldGuardPlugin mPlugin;

	private final String latestSupported = "7.0.0";
	private final String earliestSupported = "6.0.0";

	public WorldGuardCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender()
					.sendMessage(MobHunting.PREFIX_WARNING + "Compatibility with WorldGuard is disabled in config.yml");
		} else {
			mPlugin = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin(CompatPlugin.WorldGuard.getName());
			if (Servers.isMC113OrNewer()) {
				if (mPlugin.getDescription().getVersion().compareTo(latestSupported) >= 0) {
					Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX + "Enabling compatibility with WorldGuard ("
							+ mPlugin.getDescription().getVersion() + ")");
					supported = true;
				} else if (mPlugin.getDescription().getVersion().compareTo("1.16") >= 0) {
					Bukkit.getConsoleSender()
							.sendMessage(MobHunting.PREFIX + "Enabling compatibility with FastAsyncWorldGuard ("
									+ mPlugin.getDescription().getVersion() + ")");
					supported = true;
				} else {
					Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX_WARNING
							+ "Your current version of WorldGuard (" + mPlugin.getDescription().getVersion()
							+ ") is not supported by MobHunting. Please upgrade to " + latestSupported + " or newer.");
				}
			} else {
				if (mPlugin.getDescription().getVersion().compareTo(earliestSupported) >= 0) {
					Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX + "Enabling compatibility with WorldGuard ("
							+ mPlugin.getDescription().getVersion() + ")");
					supported = true;
				} else {
					Bukkit.getConsoleSender()
							.sendMessage(MobHunting.PREFIX_WARNING + "Your current version of WorldGuard ("
									+ mPlugin.getDescription().getVersion()
									+ ") is not supported by MobHunting. Please upgrade to " + earliestSupported + " or newer.");
				}
			}

		}
	}

	// **************************************************************************
	// OTHER FUNCTIONS
	// **************************************************************************
	public static WorldGuardPlugin getWorldGuardPlugin() {
		return mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationWorldGuard;
	}

	public static boolean isAllowedByWorldGuard(Entity damager, Entity damaged, StateFlag stateFlag,
			boolean defaultValue) {
		if (getWorldGuardPlugin().getDescription().getVersion().compareTo("7.0.0") >= 0) {
			return WorldGuard7Helper.isAllowedByWorldGuard2(damager, damaged, stateFlag, defaultValue);
		} else {
			return true;// WorldGuard6Helper.isAllowedByWorldGuard2(damager, damaged, stateFlag,
						// defaultValue);
		}
	}

	public static void registerFlag() {
		mPlugin = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin(CompatPlugin.WorldGuard.getName());
		if (mPlugin.getDescription().getVersion().compareTo("7.0.0") >= 0) {
			WorldGuard7Helper.registerFlag2();
		} else {
			// WorldGuard6Helper.registerFlag2();
		}

	}

}

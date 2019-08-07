package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;

import one.lindegaard.Core.Server.Servers;
import one.lindegaard.Core.compatibility.CompatPlugin;
import one.lindegaard.MobHunting.MobHunting;

public class WorldGuardCompat {

	private static boolean supported = false;
	private static WorldGuardPlugin mPlugin;

	public WorldGuardCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Compatibility with WorldGuard is disabled in config.yml");
		} else {
			mPlugin = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin(CompatPlugin.WorldGuard.getName());
			if (Servers.isMC113OrNewer()) {
				if (mPlugin.getDescription().getVersion().compareTo("7.0.0") >= 0) {
					Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
							+ "Enabling compatibility with WorldGuard (" + mPlugin.getDescription().getVersion() + ")");
					supported = true;
				} else {
					Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RED
							+ "Your current version of WorldGuard (" + mPlugin.getDescription().getVersion()
							+ ") is not supported by MobHunting. Mobhunting 6.x does only support 7.0.0 beta 1 and newer.");
				}
			} else {
				if (mPlugin.getDescription().getVersion().compareTo("6.0.0") >= 0) {
					Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
							+ "Enabling compatibility with WorldGuard (" + mPlugin.getDescription().getVersion() + ")");
					supported = true;
				} else {
					Bukkit.getConsoleSender()
							.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RED
									+ "Your current version of WorldGuard (" + mPlugin.getDescription().getVersion()
									+ ") is not supported by MobHunting. Mobhunting does only support 6.0.0 newer.");
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
			return WorldGuard6Helper.isAllowedByWorldGuard2(damager, damaged, stateFlag, defaultValue);
		}
	}
	
	public static void registerFlag() {
		mPlugin = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin(CompatPlugin.WorldGuard.getName());
		if (mPlugin.getDescription().getVersion().compareTo("7.0.0") >= 0) {
			WorldGuard7Helper.registerFlag2();
		} else {
			WorldGuard6Helper.registerFlag2();
		}
			
	}
	
	

}

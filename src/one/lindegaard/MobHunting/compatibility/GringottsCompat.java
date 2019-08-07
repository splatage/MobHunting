package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.gestern.gringotts.Gringotts;

import one.lindegaard.Core.compatibility.CompatPlugin;
import one.lindegaard.MobHunting.MobHunting;

public class GringottsCompat {

	// http://dev.bukkit.org/bukkit-plugins/gringotts/
	// Source code: https://github.com/MinecraftWars/Gringotts

	private static boolean supported = false;
	private static Gringotts mPlugin;

	public GringottsCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Compatibility with Gringotts is disabled in config.yml");
		} else {
			mPlugin = (Gringotts) Bukkit.getPluginManager().getPlugin(CompatPlugin.Gringotts.getName());

			if (mPlugin.getDescription().getVersion().compareTo("2.11") >= 0) {

				Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
						+ "Enabling Compatibility with Gringotts (" + getGringotts().getDescription().getVersion() + ")");
				supported = true;

			} else {
				Bukkit.getConsoleSender()
						.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RED
								+ "Your current version of Gringotts (" + mPlugin.getDescription().getVersion()
								+ ") has no API implemented. Please update to V2.11 or newer.");
			}
		}
	}

	// **************************************************************************
	// OTHER FUNCTIONS
	// **************************************************************************
	public static Gringotts getGringotts() {
		return mPlugin;
	}
	
	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationGringotts;
	}

}

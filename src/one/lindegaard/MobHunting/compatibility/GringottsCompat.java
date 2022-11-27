package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.gestern.gringotts.Gringotts;

import one.lindegaard.CustomItemsLib.compatibility.CompatPlugin;
import one.lindegaard.MobHunting.MobHunting;

public class GringottsCompat {

	// http://dev.bukkit.org/bukkit-plugins/gringotts/
	// Source code: https://github.com/MinecraftWars/Gringotts

	private static boolean supported = false;
	private static Gringotts mPlugin;

	public GringottsCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender()
					.sendMessage(MobHunting.PREFIX_WARNING + "Compatibility with Gringotts is disabled in config.yml");
		} else {
			mPlugin = (Gringotts) Bukkit.getPluginManager().getPlugin(CompatPlugin.Gringotts.getName());

			if (mPlugin.getDescription().getVersion().compareTo("2.11") >= 0) {

				Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX + "Enabling Compatibility with Gringotts ("
						+ getGringotts().getDescription().getVersion() + ")");
				supported = true;

			} else {
				Bukkit.getConsoleSender()
						.sendMessage(MobHunting.PREFIX_WARNING + "Your current version of Gringotts ("
								+ mPlugin.getDescription().getVersion()
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

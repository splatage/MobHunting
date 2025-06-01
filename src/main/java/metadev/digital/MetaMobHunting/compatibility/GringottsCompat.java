package metadev.digital.MetaMobHunting.compatibility;

import org.bukkit.Bukkit;
import org.gestern.gringotts.Gringotts;

import metadev.digital.metacustomitemslib.compatibility.CompatPlugin;
import metadev.digital.MetaMobHunting.MobHunting;

public class GringottsCompat {

	// http://dev.bukkit.org/bukkit-plugins/gringotts/
	// Source code: https://github.com/MinecraftWars/Gringotts

	private static boolean supported = false;
	private static Gringotts mPlugin;
	private final String latestSupported = "2.11";

	public GringottsCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender()
					.sendMessage(MobHunting.PREFIX_WARNING + "Compatibility with Gringotts is disabled in config.yml");
		} else {
			mPlugin = (Gringotts) Bukkit.getPluginManager().getPlugin(CompatPlugin.Gringotts.getName());

			if (mPlugin.getDescription().getVersion().compareTo(latestSupported) >= 0) {

				Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX + "Enabling Compatibility with Gringotts ("
						+ getGringotts().getDescription().getVersion() + ")");
				supported = true;

			} else {
				Bukkit.getConsoleSender()
						.sendMessage(MobHunting.PREFIX_WARNING + "Your current version of Gringotts ("
								+ mPlugin.getDescription().getVersion()
								+ ") is not supported by MobHunting. Please upgrade to " + latestSupported + " or newer.");
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

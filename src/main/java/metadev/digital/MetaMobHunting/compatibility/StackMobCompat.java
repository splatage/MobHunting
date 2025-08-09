package metadev.digital.MetaMobHunting.compatibility;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.MobHunting;

public class StackMobCompat implements Listener {

	// https://www.spigotmc.org/resources/stackmob.29999/

	private static boolean supported = false;
	private static Plugin mPlugin;
	private final String latestSupported = "2.0.9";

	public StackMobCompat() {
		if (!isEnabledInConfig()) {
			MessageHelper.warning("Compatibility with StackMob is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.StackMob.getName());
			if (mPlugin.getDescription().getVersion().compareTo(latestSupported) >= 0) {
				MessageHelper.notice("Enabling compatibility with StackMob ("
						+ mPlugin.getDescription().getVersion() + ").");
				Bukkit.getPluginManager().registerEvents(this, MobHunting.getInstance());
				supported = true;
			} else {
				MessageHelper.warning("Your current version of StackMob ("
								+ mPlugin.getDescription().getVersion()
								+ ") is not supported by MobHunting. Please upgrade to " + latestSupported + " or newer.");
			}
		}
	}

	// **************************************************************************
	// OTHER FUNCTIONS
	// **************************************************************************

	public static Plugin getPlugin() {
		return mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	private static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationStackMob;
	}

}

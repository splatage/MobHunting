package metadev.digital.MetaMobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import metadev.digital.metacustomitemslib.compatibility.CompatPlugin;
import metadev.digital.MetaMobHunting.MobHunting;

public class StackMobCompat implements Listener {

	// https://www.spigotmc.org/resources/stackmob.29999/

	private static boolean supported = false;
	private static Plugin mPlugin;

	public StackMobCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender()
					.sendMessage(MobHunting.PREFIX_WARNING + "Compatibility with StackMob is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(CompatPlugin.StackMob.getName());
			if (mPlugin.getDescription().getVersion().compareTo("2.0.9") >= 0) {
				Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX + "Enabling compatibility with StackMob ("
						+ mPlugin.getDescription().getVersion() + ").");
				Bukkit.getPluginManager().registerEvents(this, MobHunting.getInstance());
				supported = true;
			} else {
				Bukkit.getServer().getConsoleSender()
						.sendMessage(MobHunting.PREFIX_WARNING + "Your current version of StackMob ("
								+ mPlugin.getDescription().getVersion()
								+ ") is not supported by MobHunting, please upgrade to 2.0.9 or newer.");
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

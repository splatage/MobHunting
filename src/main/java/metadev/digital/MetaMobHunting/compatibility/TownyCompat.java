/**
 * TODO: Migrate to Towny Advanced

package metadev.digital.MetaMobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import metadev.digital.metacustomitemslib.compatibility.CompatPlugin;
import metadev.digital.MetaMobHunting.MobHunting;

public class TownyCompat {

	private static Plugin mPlugin;
	private static boolean supported = false;

	// http://towny.palmergames.com/

	public TownyCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender()
					.sendMessage(MobHunting.PREFIX_WARNING + "Compatibility with Towny is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(CompatPlugin.Towny.getName());

			try {
				@SuppressWarnings({ "rawtypes", "unused" })
				Class cls = Class.forName("com.palmergames.bukkit.towny.object.TownyUniverse");
				Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX + "Enabling compatibility with Towny ("
						+ mPlugin.getDescription().getVersion() + ").");
				supported = true;
			} catch (ClassNotFoundException e) {
				Bukkit.getConsoleSender()
						.sendMessage(MobHunting.PREFIX_WARNING + "Your version of Towny ("
								+ mPlugin.getDescription().getVersion()
								+ ") is not complatible with this version of MobHunting, please upgrade.");
			}
		}
	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public Plugin getPlugin() {
		return mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationTowny;
	}

	public static boolean isInHomeTown(Player player) {
		if (supported) {
			return TownyHelper.isInHomeTown(player);
		}
		return false;
	}

	public static boolean isInAnyTown(Player player) {
		if (supported) {
			return TownyHelper.isInAnyTomn(player);
		}
		return false;
	}

}
 */
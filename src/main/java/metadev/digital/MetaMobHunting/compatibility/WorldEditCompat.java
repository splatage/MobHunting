package metadev.digital.MetaMobHunting.compatibility;

import org.bukkit.Bukkit;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;

import metadev.digital.metacustomitemslib.server.Server;
import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.MobHunting;

public class WorldEditCompat {
	private static WorldEditPlugin mPlugin;
	private static boolean supported = false;
	private final String latestSupported = "7.0.0";
	private final String earliestSupported = "6.1.0";

	public WorldEditCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender()
					.sendMessage(MobHunting.PREFIX + "Compatibility with WorldEdit is disabled in config.yml");
		} else {
			mPlugin = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.WorldEdit.getName());
			if (Server.isMC113OrNewer()) {
				if (mPlugin.getDescription().getVersion().compareTo(latestSupported) >= 0) {
					Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX + "Enabling compatibility with WorldEdit ("
							+ mPlugin.getDescription().getVersion() + ")");
					supported = true;
				} else if (mPlugin.getDescription().getVersion().compareTo("1.16") >= 0) {
					Bukkit.getConsoleSender()
							.sendMessage(MobHunting.PREFIX + "Enabling compatibility with FastAsyncWorldEdit ("
									+ mPlugin.getDescription().getVersion() + ")");
					supported = true;
				} else {
					Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX_WARNING
							+ "Your current version of WorldEdit (" + mPlugin.getDescription().getVersion()
							+ ") is not supported by MobHunting. Please upgrade to " + latestSupported + " or newer.");
				}
			} else {
				if (mPlugin.getDescription().getVersion().compareTo(earliestSupported) >= 0) {
					Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX + "Enabling compatibility with WorldEdit ("
							+ mPlugin.getDescription().getVersion() + ")");
					supported = true;
				} else {
					Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX_WARNING
							+ "Your current version of WorldEdit (" + mPlugin.getDescription().getVersion()
							+ ") is not supported by MobHunting. Please upgrade to " + earliestSupported + " or newer.");
				}
			}
		}
	}

	public static WorldEditPlugin getWorldEdit() {
		return mPlugin;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationWorldEdit;
	}

	public static boolean isSupported() {
		return supported;
	}
}

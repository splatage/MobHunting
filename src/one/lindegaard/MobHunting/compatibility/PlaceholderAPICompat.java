package one.lindegaard.MobHunting.compatibility;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import me.clip.placeholderapi.PlaceholderAPI;
import one.lindegaard.CustomItemsLib.compatibility.CompatPlugin;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.placeholder.MobHuntingPlaceholderExpansion;
import one.lindegaard.MobHunting.placeholder.PlaceHolderData;
import one.lindegaard.MobHunting.placeholder.PlaceHolderManager;

public class PlaceholderAPICompat {

	private static Plugin mPlugin;
	private static boolean supported = false;
	private static PlaceHolderManager mPlaceHolderManager;

	// https://www.spigotmc.org/resources/placeholderapi.6245/
	// https://github.com/PlaceholderAPI/PlaceholderAPI/wiki/PlaceholderExpansion#without-external-plugin

	public PlaceholderAPICompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender().sendMessage(
					MobHunting.PREFIX_WARNING + "Compatibility with PlaceholderAPI is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(CompatPlugin.PlaceholderAPI.getName());
			if (mPlugin.getDescription().getVersion().compareTo("2.11.1") >= 0) {
				Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX + "Enabling compatibility with PlaceholderAPI ("
						+ mPlugin.getDescription().getVersion() + ").");
				new MobHuntingPlaceholderExpansion().register();
				mPlaceHolderManager = new PlaceHolderManager(MobHunting.getInstance());
				supported = true;
			} else {
				Bukkit.getConsoleSender()
						.sendMessage(MobHunting.PREFIX_WARNING + "Your current version of PlaceholderAPI ("
								+ mPlugin.getDescription().getVersion()
								+ ") is not supported by MobHunting, please upgrade to 2.11.1 or newer.");
			}
		}
	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationPlaceholderAPI;
	}

	public static HashMap<UUID, PlaceHolderData> getPlaceHolders() {
		return mPlaceHolderManager.getPlaceHolders();
	}

	public static void shutdown() {
		mPlaceHolderManager.shutdown();
	}

	public static String setPlaceholders(Player player, String messages_with_placeholders) {
		if (isSupported())
			return PlaceholderAPI.setPlaceholders(player, messages_with_placeholders);
		return messages_with_placeholders;
	}

}

package metadev.digital.MetaMobHunting.compatibility;

import java.util.HashMap;
import java.util.UUID;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import me.clip.placeholderapi.PlaceholderAPI;
import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.placeholder.MobHuntingPlaceholderExpansion;
import metadev.digital.MetaMobHunting.placeholder.PlaceHolderData;
import metadev.digital.MetaMobHunting.placeholder.PlaceHolderManager;

public class PlaceholderAPICompat {

	private static Plugin mPlugin;
	private static boolean supported = false;
	private static PlaceHolderManager mPlaceHolderManager;
	private final String latestSupported = "2.11.6";

	// https://www.spigotmc.org/resources/placeholderapi.6245/
	// https://github.com/PlaceholderAPI/PlaceholderAPI/wiki/PlaceholderExpansion#without-external-plugin

	public PlaceholderAPICompat() {
		if (!isEnabledInConfig()) {
			MessageHelper.warning("Compatibility with PlaceholderAPI is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.PlaceholderAPI.getName());
			if (mPlugin.getDescription().getVersion().compareTo(latestSupported) >= 0) {
				MessageHelper.notice("Enabling compatibility with PlaceholderAPI ("
						+ mPlugin.getDescription().getVersion() + ").");
				new MobHuntingPlaceholderExpansion().register();
				mPlaceHolderManager = new PlaceHolderManager(MobHunting.getInstance());
				supported = true;
			} else {
				MessageHelper.warning("Your current version of PlaceholderAPI ("
								+ mPlugin.getDescription().getVersion()
								+ ") is not supported by MobHunting. Please upgrade to " + latestSupported + " or newer.");
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

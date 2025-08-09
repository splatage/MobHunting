package metadev.digital.MetaMobHunting.compatibility;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import org.bukkit.Bukkit;

import net.citizensnpcs.api.CitizensAPI;
import metadev.digital.metabagofgold.BagOfGold;
import metadev.digital.metabagofgold.bank.BankManager;
import metadev.digital.metabagofgold.storage.DataStoreManager;
import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.MobHunting;

public class BagOfGoldCompat {

	private BagOfGold mPlugin;
	private static boolean supported = false;
	private final String latestSupported = "4.5.7";

	public BagOfGoldCompat() {
		mPlugin = (BagOfGold) Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.BagOfGold.getName());

		if (mPlugin.getDescription().getVersion().compareTo(latestSupported) >= 0) {
			MessageHelper.notice("Enabling compatibility with BagOfGold ("
							+ getBagOfGoldAPI().getDescription().getVersion() + ")");
			supported = true;
		} else {
			MessageHelper.warning("Your current version of BagOfGold ("
							+ mPlugin.getDescription().getVersion()
							+ ") is not supported by MobHunting. Please upgrade to " + latestSupported + " or newer.");
			Bukkit.getPluginManager().disablePlugin(mPlugin);
		}

	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public BagOfGold getBagOfGoldAPI() {
		return mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean useAsEconomyAnEconomyPlugin() {
		return supported && BagOfGold.getInstance().getConfigManager().useBagOfGoldAsAnEconomyPlugin;
	}

	public String getBagOfGoldFormat() {
		return Core.getConfigManager().numberFormat;
	}

	public DataStoreManager getDataStoreManager() {
		return BagOfGold.getInstance().getDataStoreManager();
	}

	public BankManager getBankManager() {
		return BagOfGold.getInstance().getBankManager();
	}

	public static boolean isNPC(Integer id) {
		if (isSupported())
			return CitizensAPI.getNPCRegistry().getById(id) != null;
		return false;
	}

}

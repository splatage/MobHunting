package metadev.digital.MetaMobHunting.compatibility;

import org.bukkit.Bukkit;

import net.citizensnpcs.api.CitizensAPI;
import metadev.digital.metabagofgold.BagOfGold;
import metadev.digital.metabagofgold.bank.BankManager;
import metadev.digital.metabagofgold.storage.DataStoreManager;
import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.compatibility.CompatPlugin;
import metadev.digital.MetaMobHunting.MobHunting;

public class BagOfGoldCompat {

	private BagOfGold mPlugin;
	private static boolean supported = false;

	public BagOfGoldCompat() {
		mPlugin = (BagOfGold) Bukkit.getPluginManager().getPlugin(CompatPlugin.BagOfGold.getName());

		if (mPlugin.getDescription().getVersion().compareTo("4.5.1") >= 0) {
			Bukkit.getServer().getConsoleSender()
					.sendMessage(MobHunting.PREFIX + "Enabling compatibility with BagOfGold ("
							+ getBagOfGoldAPI().getDescription().getVersion() + ")");
			supported = true;
		} else {
			Bukkit.getServer().getConsoleSender()
					.sendMessage(MobHunting.PREFIX_WARNING + "Your current version of BagOfGold ("
							+ mPlugin.getDescription().getVersion()
							+ ") is outdated. Please upgrade to 4.5.1 or newer.");
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

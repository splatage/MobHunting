package one.lindegaard.MobHunting;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;
import net.tnemc.core.Reserve;
import net.tnemc.core.economy.EconomyAPI;
import net.tnemc.core.economy.ExtendedEconomyAPI;

/**
 * Economy handler to interface with Vault or Reserve directly.
 * 
 * @author Rocologo
 * 
 */
public class MobHuntingEconomyHandler {

	private static MobHunting plugin = null;

	private static Economy vaultEconomy = null;

	private static EconomyAPI reserveEconomy = null;

	private EcoType Type = EcoType.NONE;

	private String version = "";

	public enum EcoType {
		NONE, VAULT, RESERVE
	}

	public MobHuntingEconomyHandler(MobHunting plugin) {
		this.plugin = plugin;
	}

	/**
	 * @return the economy type we have detected.
	 */
	public EcoType getType() {

		return Type;
	}

	/**
	 * Are we using any economy system?
	 * 
	 * @return true if we found one.
	 */
	public boolean isActive() {

		return (Type != EcoType.NONE);
	}

	/**
	 * @return The current economy providers version string
	 */
	public String getVersion() {

		return version;
	}

	/**
	 * Internal function to set the version string.
	 * 
	 * @param version
	 */
	private void setVersion(String version) {

		this.version = version;
	}

	/**
	 * Find and configure a suitable economy provider
	 * 
	 * @return true if successful.
	 */
	public Boolean setupEconomy() {

		Plugin economyProvider = null;

		/*
		 * Attempt to find Vault for Economy handling
		 */
		try {
			RegisteredServiceProvider<Economy> vaultEcoProvider = plugin.getServer().getServicesManager()
					.getRegistration(net.milkbowl.vault.economy.Economy.class);
			if (vaultEcoProvider != null) {
				/*
				 * Flag as using Vault hooks
				 */
				vaultEconomy = vaultEcoProvider.getProvider();
				setVersion(String.format("%s %s", vaultEcoProvider.getProvider().getName(), "via Vault"));
				Type = EcoType.VAULT;
				return true;
			}
		} catch (NoClassDefFoundError ex) {
		}

		/*
		 * Attempt to find Reserve for Economy handling
		 */
		economyProvider = plugin.getServer().getPluginManager().getPlugin("Reserve");
		if (economyProvider != null && ((Reserve) economyProvider).economyProvided()) {
			/*
			 * Flat as using Reserve Hooks.
			 */
			reserveEconomy = ((Reserve) economyProvider).economy();
			setVersion(String.format("%s %s", reserveEconomy.name(), "via Reserve"));
			Type = EcoType.RESERVE;
			return true;
		}

		/*
		 * No compatible Economy system found.
		 */
		return false;
	}

	/**
	 * Returns the relevant player's economy account
	 * 
	 * @param accountName - Name of the player's account (usually playername)
	 * @return - The relevant player's economy account
	 */
	private Object getEconomyAccount(String accountName) {

		switch (Type) {

		case RESERVE:
			if (reserveEconomy instanceof ExtendedEconomyAPI)
				return ((ExtendedEconomyAPI) reserveEconomy).getAccount(accountName);
			break;

		default:
			break;

		}

		return null;
	}

}

package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.blueskullgames.horserpg.HorseRPG;
import com.blueskullgames.horserpg.RPGHorse;

import one.lindegaard.CustomItemsLib.compatibility.CompatPlugin;
import one.lindegaard.MobHunting.MobHunting;

public class McMMOHorses {

	// https://www.spigotmc.org/resources/mcmmohorses.46301/
	
	private static HorseRPG mPlugin;
	private static boolean supported = false;

	public McMMOHorses() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Compatibility with McMMOHorses is disabled in config.yml");
		} else {
			mPlugin = (HorseRPG) Bukkit.getPluginManager().getPlugin(CompatPlugin.McMMOHorses.getName());

			if (mPlugin.getDescription().getVersion().compareTo("4.3.55") >= 0) {
				Bukkit.getConsoleSender()
				.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
						+ "Enabling compatibility with McMMOHorses ("
						+ getMcMMOHorses().getDescription().getVersion() + ")");
				supported = true;
			} else {
				Bukkit.getConsoleSender()
						.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RED
								+ "Your current version of McMMOHorses (" + mPlugin.getDescription().getVersion()
								+ ") is not supported by MobHunting, please upgrade to 4.3.55 or newer.");
			}
		}
	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public static HorseRPG getMcMMOHorses() {
		return mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationMcMMOHorses;
	}

	public static RPGHorse getHorse(Entity entity) {
		if (isSupported()) {
			return HorseRPG.getHorse(entity);
		} else
			return null;
	}

	public static boolean isMcMMOHorse(Entity entity) {
		if (isSupported()) {
			return HorseRPG.isRPGHorse(entity);
		} else
			return false;
	}
	
	public static boolean isMcMMOHorseOwner(Entity entity, Player player) {
		if (isSupported()) {
			return HorseRPG.getHorse(entity).owners_name.equalsIgnoreCase(player.getName());
		} else
			return false;
	}

	public static boolean isGodmode(Entity entity) {
		if (isSupported()) {
			return HorseRPG.getHorse(entity).godmode;
		} else
			return false;
	}

	public static boolean isPermanentDeath() {
		if (isSupported()) {
			return HorseRPG.permanentDeath;
		} else
			return false;
	}
	
	public static RPGHorse getCurrentHorse(Player player) {
		if (isSupported()) {
			return HorseRPG.pCurrentHorse.get(player.getUniqueId());
		} else
			return null;
	}
}

package metadev.digital.MetaMobHunting.compatibility;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import org.battleplugins.arena.event.player.ArenaJoinEvent;
import org.battleplugins.arena.event.player.ArenaLeaveEvent;
import org.battleplugins.arena.ArenaPlayer;

import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.MobHunting;

public class BattleArenaCompat implements Listener {

	private static Plugin mPlugin;
	private static List<UUID> playersPlayingBattleArena = new ArrayList<UUID>();
	private static boolean supported = false;

	public BattleArenaCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender().sendMessage(
					MobHunting.PREFIX_WARNING + "Compatibility with BattleArena is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.BattleArena.getName());

			Bukkit.getPluginManager().registerEvents(this, MobHunting.getInstance());

			Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX + "Enabling compatibility with BattleArena ("
					+ getBattleArena().getDescription().getVersion() + ")");
			supported = true;
		}
	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public Plugin getBattleArena() {
		return mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationBattleArena;
	}

	/**
	 * Determine if the player is currently playing BattleArena
	 * 
	 * @param player
	 * @return Returns true when the player is in game.
	 */
	public static boolean isPlayingBattleArena(Player player) {
		if (isSupported())
			return playersPlayingBattleArena.contains(player.getUniqueId());
		return false;
	}

	/**
	 * Add the player to the list of active BattleArena players.
	 * 
	 * @param arenaPlayer
	 */
	public static void startPlayingBattleArena(ArenaPlayer arenaPlayer) {
		playersPlayingBattleArena.add(arenaPlayer.getPlayer().getUniqueId());
	}
	/**
	 * Remove the player from list of active BattleArena players
	 * 
	 * @param arenaPlayer
	 */
	public static void stopPlayingBattleArena(ArenaPlayer arenaPlayer) {
		if (!playersPlayingBattleArena.remove(arenaPlayer.getPlayer().getUniqueId())) {
			MobHunting.getInstance().getMessages().debug("Player: %s is not a the BattleArena", arenaPlayer.getPlayer().getName());
		}
	}

	// **************************************************************************
	// EVENTS
	// **************************************************************************
	@EventHandler(priority = EventPriority.NORMAL)
	private void onArenaPlayerJoinEvent(ArenaJoinEvent event) {
		MobHunting.getInstance().getMessages().debug("BattleArenaCompat.StartEvent s%", event.getEventName());
		startPlayingBattleArena(event.getArenaPlayer());
	}

	@EventHandler(priority = EventPriority.NORMAL)
	private void onArenaPlayerLeaveEvent(ArenaLeaveEvent event) {
		MobHunting.getInstance().getMessages().debug("BattleArenaCompat.StartEvent %s", event.getEventName());
		stopPlayingBattleArena(event.getArenaPlayer());
	}

}

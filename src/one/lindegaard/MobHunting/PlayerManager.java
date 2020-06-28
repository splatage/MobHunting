package one.lindegaard.MobHunting;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import one.lindegaard.Core.Core;
import one.lindegaard.Core.PlayerSettings;
import one.lindegaard.Core.storage.DataStoreException;
import one.lindegaard.Core.storage.IDataCallback;
import one.lindegaard.MobHunting.rewards.CustomItems;

public class PlayerManager implements Listener {

	private HashMap<UUID, PlayerSettings> mPlayerData = new HashMap<UUID, PlayerSettings>();

	private MobHunting plugin;

	/**
	 * Constructor for the PlayerSettingsmanager
	 */
	PlayerManager(MobHunting plugin) {
		this.plugin = plugin;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Get playerSettings from memory
	 * 
	 * @param offlinePlayer
	 * @return PlayerSettings
	 */
	public PlayerSettings getPlayerData(OfflinePlayer offlinePlayer) {
		if (mPlayerData.containsKey(offlinePlayer.getUniqueId()))
			return mPlayerData.get(offlinePlayer.getUniqueId());
		else {
			PlayerSettings ps;
			try {
				ps = Core.getStoreManager().loadPlayerSettings(offlinePlayer);
			} catch (DataStoreException e) {
				return new PlayerSettings(offlinePlayer);
			}
			return ps;
		}

	}

	/**
	 * Store playerSettings in memory
	 * 
	 * @param playerSettings
	 */
	public void setPlayerData(OfflinePlayer player, PlayerSettings playerSettings) {
		mPlayerData.put(player.getUniqueId(), playerSettings);
		Core.getDataStoreManager().insertPlayerSettings(playerSettings);
	}

	/**
	 * Remove PlayerSettings from Memory
	 * 
	 * @param player
	 */
	public void removePlayerData(OfflinePlayer player) {
		plugin.getMessages().debug("Removing %s from player settings cache", player.getName());
		mPlayerData.remove(player.getUniqueId());
	}

	/**
	 * Read PlayerSettings From database into Memory when player joins
	 * 
	 * @param event
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	private void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		if (containsKey(player))
			plugin.getMessages().debug("Using cached player settings for %s. Balance=%s (%s)", player.getName(),
					plugin.getRewardManager().format(plugin.getRewardManager().getBalance(player)),
					player.getGameMode());
		else {
			Core.getPlayerSettingsManager().load(player);
			load(player);
		}
	}

	/**
	 * Write PlayerSettings to Database when Player Quit and remove PlayerSettings
	 * from memory
	 * 
	 * @param event
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	private void onPlayerQuit(PlayerQuitEvent event) {
		final Player player = event.getPlayer();
		Core.getDataStoreManager().insertPlayerSettings(getPlayerData(player));
	}

	/**
	 * Load PlayerSettings asynchronously from Database
	 * 
	 * @param player
	 */
	public void load(final OfflinePlayer player) {
		Core.getDataStoreManager().requestPlayerSettings(player, new IDataCallback<PlayerSettings>() {

			@Override
			public void onCompleted(PlayerSettings ps) {
				mPlayerData.put(player.getUniqueId(), ps);

				if (ps.isMuted())
					plugin.getMessages().debug("%s isMuted()", player.getName());
				if (ps.isLearningMode())
					plugin.getMessages().debug("%s is in LearningMode()", player.getName());

				if (ps.getTexture() == null || ps.getTexture().equals("")) {
					plugin.getMessages().debug("Store %s skin in MobHunting Skin Cache", player.getName());
					new CustomItems().getPlayerHead(player.getUniqueId(), player.getName(), 1, 0);
				}
			}

			@Override
			public void onError(Throwable error) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[MobHunting][ERROR] " + player.getName()
						+ " is new, creating user in database.");
				mPlayerData.put(player.getUniqueId(), new PlayerSettings(player));
			}
		});
	}

	/**
	 * Test if PlayerSettings contains data for Player
	 * 
	 * @param player
	 * @return true if player exists in PlayerSettings in Memory
	 */
	public boolean containsKey(final OfflinePlayer player) {
		return mPlayerData.containsKey(player.getUniqueId());
	}

}

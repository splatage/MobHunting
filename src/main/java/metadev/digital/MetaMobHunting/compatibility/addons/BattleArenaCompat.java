package metadev.digital.MetaMobHunting.compatibility.addons;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import metadev.digital.metacustomitemslib.compatibility.Feature;
import metadev.digital.metacustomitemslib.compatibility.FeatureList;
import metadev.digital.metacustomitemslib.compatibility.ICompat;
import metadev.digital.metacustomitemslib.compatibility.IFeatureHolder;
import metadev.digital.metacustomitemslib.compatibility.enums.BoundIdentifierEnum;
import metadev.digital.metacustomitemslib.compatibility.enums.VersionSetIdentifierEnum;
import metadev.digital.metacustomitemslib.compatibility.exceptions.FeatureNotFoundException;
import metadev.digital.metacustomitemslib.compatibility.exceptions.SpinupShutdownException;
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

public class BattleArenaCompat implements Listener, ICompat, IFeatureHolder {

    // ****** Standard ******
	private Plugin compatPlugin;
    private static boolean enabled = false, supported = false, loaded = false;
    private static String sMin, sMax, pMin, pMax;
    private static FeatureList features;

    // ****** Plugin Specific ******
    private static List<UUID> playersPlayingBattleArena = new ArrayList<UUID>();

	public BattleArenaCompat() {
        compatPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.BattleArena.getName());

        if(compatPlugin != null) {
            try {
                start();
            } catch (SpinupShutdownException e) {
                Bukkit.getPluginManager().disablePlugin(compatPlugin);
            }
        }
    }

    // ****** ICompat ******

    @Override
    public void start() throws SpinupShutdownException {
        detectedMessage();
        registerFeatures();

        if (isActive()) {
            Bukkit.getPluginManager().registerEvents(this, MobHunting.getInstance());
            successfullyLoadedMessage();
            loaded = true;
        } else if (enabled && !supported) {
            Feature base = getFeature("base");
            if(base != null) unsupportedMessage(base);
            else pluginError("Plugin is enabled but not supported, and failed to understand the reasoning out of the base " +
                    "feature. Likely caused by a corrupt / incorrect construction of the base feature.");
            throw new SpinupShutdownException();
        }
    }

    @Override
    public void shutdown() throws SpinupShutdownException {
        if (isActive() && loaded) {
            successfullyShutdownMessage();
            loaded = false;
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isSupported() {
        return supported;
    }

    @Override
    public boolean isActive() {
        return enabled && supported;
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public Plugin getPluginInstance() {
        return compatPlugin;
    }

    @Override
    public String getPluginName() {
        return compatPlugin.getName();
    }

    @Override
    public String getPluginVersion() {
        return compatPlugin.getDescription().getVersion();
    }

    // ****** IFeatureHolder ******

    @Override
    public void registerFeatures() {
        features = new FeatureList(getPluginVersion());

        // Base plugin
        enabled = MobHunting.getInstance().getConfigManager().enableIntegrationBattleArena;
        features.addFeature("base", enabled);
        supported = isFeatureSupported("base");

        // Other features
    }

    @Override
    public boolean isFeatureEnabled(String name) {
        boolean featureEnabled = false;
        try {
            featureEnabled = features.isFeatureEnabled(name);
        } catch (FeatureNotFoundException e) {
            MessageHelper.debug("Triggered a FeatureNotFoundException when trying to return enable flag of the feature " + name + " in the " + compatPlugin.getName() +" compat class." );
        }

        return featureEnabled;
    }

    @Override
    public boolean isFeatureSupported(String name) {
        boolean featureSupported = false;
        try {
            featureSupported = features.isFeatureSupported(name);
        } catch (FeatureNotFoundException e) {
            MessageHelper.debug("Triggered a FeatureNotFoundException when trying to return supported flag of the feature " + name + " in the " + compatPlugin.getName() +" compat class." );
        }

        return featureSupported;
    }

    @Override
    public boolean isFeatureActive(String name) {
        boolean featureActive = false;
        try {
            featureActive = features.isFeatureActive(name);
        } catch (FeatureNotFoundException e) {
            MessageHelper.debug("Triggered a FeatureNotFoundException when trying to return active flag of the feature " + name + " in the " + compatPlugin.getName() +" compat class." );
        }

        return featureActive;
    }

    @Override
    public Feature getFeature(String name) {
        Feature feature;
        try {
            feature = features.getFeature(name);
            return feature;
        } catch (FeatureNotFoundException e) {
            MessageHelper.debug("Triggered a FeatureNotFoundException when trying to return the feature " + name + " in the " + compatPlugin.getName() +" compat class." );
        }
        return null;
    }

    // ****** Listener ******
	@EventHandler(priority = EventPriority.NORMAL)
	private void onArenaPlayerJoinEvent(ArenaJoinEvent event) {
		MessageHelper.debug("BattleArenaCompat.StartEvent s%", event.getEventName());
		startPlayingBattleArena(event.getArenaPlayer());
	}

	@EventHandler(priority = EventPriority.NORMAL)
	private void onArenaPlayerLeaveEvent(ArenaLeaveEvent event) {
		MessageHelper.debug("BattleArenaCompat.StartEvent %s", event.getEventName());
		stopPlayingBattleArena(event.getArenaPlayer());
	}

    // ****** Plugin Specific ******

    public Plugin getBattleArena() {
        return compatPlugin;
    }

    /**
     * Determine if the player is currently playing BattleArena
     *
     * @param player
     * @return Returns true when the player is in game.
     */
    public static boolean isPlayingBattleArena(Player player) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.BagOfGold.getName())))
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
            MessageHelper.debug("Player: %s is not a the BattleArena", arenaPlayer.getPlayer().getName());
        }
    }
}

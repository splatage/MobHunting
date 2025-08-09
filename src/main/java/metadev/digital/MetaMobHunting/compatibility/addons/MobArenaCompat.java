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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import com.garbagemule.MobArena.events.ArenaPlayerDeathEvent;
import com.garbagemule.MobArena.events.ArenaPlayerJoinEvent;
import com.garbagemule.MobArena.events.ArenaPlayerLeaveEvent;
import com.garbagemule.MobArena.events.ArenaKillEvent;
import com.garbagemule.MobArena.events.ArenaCompleteEvent;
import com.garbagemule.MobArena.events.ArenaEndEvent;
import com.garbagemule.MobArena.events.ArenaPlayerReadyEvent;
import com.garbagemule.MobArena.events.ArenaStartEvent;
import com.garbagemule.MobArena.events.NewWaveEvent;

import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.MobHunting;

public class MobArenaCompat implements Listener, ICompat, IFeatureHolder {

    // ****** Standard ******
    private Plugin compatPlugin;
    private static boolean enabled = false, supported = false, loaded = false;
    private static String sMin, sMax, pMin = "0.109", pMax;
    private static FeatureList features;

    // ****** Plugin Specific ******
    private static List<UUID> playersPlayingMobArena = new ArrayList<UUID>();

    public MobArenaCompat() {
        compatPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.MobArena.getName());

        if (compatPlugin != null) {
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
            if (base != null) unsupportedMessage(base);
            else
                pluginError("Plugin is enabled but not supported, and failed to understand the reasoning out of the base " +
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
        enabled = MobHunting.getInstance().getConfigManager().enableIntegrationMobArena;
        features.addFeature("base", pMin, BoundIdentifierEnum.FLOOR, VersionSetIdentifierEnum.PLUGIN, enabled);
        supported = isFeatureSupported("base");

        // Other features
    }

    @Override
    public boolean isFeatureEnabled(String name) {
        boolean featureEnabled = false;
        try {
            featureEnabled = features.isFeatureEnabled(name);
        } catch (FeatureNotFoundException e) {
            MessageHelper.debug("Triggered a FeatureNotFoundException when trying to return enable flag of the feature " + name + " in the " + compatPlugin.getName() + " compat class.");
        }

        return featureEnabled;
    }

    @Override
    public boolean isFeatureSupported(String name) {
        boolean featureSupported = false;
        try {
            featureSupported = features.isFeatureSupported(name);
        } catch (FeatureNotFoundException e) {
            MessageHelper.debug("Triggered a FeatureNotFoundException when trying to return supported flag of the feature " + name + " in the " + compatPlugin.getName() + " compat class.");
        }

        return featureSupported;
    }

    @Override
    public boolean isFeatureActive(String name) {
        boolean featureActive = false;
        try {
            featureActive = features.isFeatureActive(name);
        } catch (FeatureNotFoundException e) {
            MessageHelper.debug("Triggered a FeatureNotFoundException when trying to return active flag of the feature " + name + " in the " + compatPlugin.getName() + " compat class.");
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
            MessageHelper.debug("Triggered a FeatureNotFoundException when trying to return the feature " + name + " in the " + compatPlugin.getName() + " compat class.");
        }
        return null;
    }

    // ****** Listener ******
    // Happens when the player joins the Arena /ma join
    @EventHandler(priority = EventPriority.NORMAL)
    private void onArenaPlayerJoinEvent(ArenaPlayerJoinEvent event) {
        MessageHelper.debug("Player %s joined MobArena: %s", event.getPlayer().getName(),
                event.getArena());
        startPlayingMobArena(event.getPlayer());
    }

    // Happens when the player leave the Arena /ma leave
    @EventHandler(priority = EventPriority.NORMAL)
    private void onArenaPlayerLeaveEvent(ArenaPlayerLeaveEvent event) {
        MessageHelper.debug("Player %s left MobArena: %s", event.getPlayer().getName(),
                event.getArena());
        stopPlayingMobArena(event.getPlayer());
    }

    // Happens when the player dies
    @EventHandler(priority = EventPriority.NORMAL)
    private void onArenaPlayerDeathEvent(ArenaPlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (playersPlayingMobArena.remove(player.getUniqueId())) {
            MessageHelper.debug("Player: %s died while playing MobArena", player.getName());
        }
    }

    // Happens when the player hits the Iron block (waiting for other player to
    // do the same)
    @EventHandler(priority = EventPriority.NORMAL)
    private void onArenaPlayerReadyEvent(ArenaPlayerReadyEvent event) {
    }

    // Happens when???
    @EventHandler(priority = EventPriority.NORMAL)
    private void onArenaCompleteEvent(ArenaCompleteEvent event) {
    }

    // Happens when a/the player kill a Mob
    @EventHandler(priority = EventPriority.NORMAL)
    private void onArenaKillEvent(ArenaKillEvent event) {
    }

    // Happens when the all players are ready and they enter the Arena
    @EventHandler(priority = EventPriority.NORMAL)
    private void onArenaStartEvent(ArenaStartEvent event) {
    }

    // Happens when the all players are dead and in "Jail"
    @EventHandler(priority = EventPriority.NORMAL)
    private void onArenaEndEvent(ArenaEndEvent event) {
    }

    // Happens everytime a new wave begin
    @EventHandler(priority = EventPriority.NORMAL)
    private void onNewWareEvent(NewWaveEvent event) {
    }

    @EventHandler(priority = EventPriority.NORMAL)
    private void onPlayerQuitEvent(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (playersPlayingMobArena.remove(player.getUniqueId())) {
            MessageHelper.debug("Player: %s left the game while playing MobArena",
                    player.getName());
        }
    }

    // ****** Plugin Specific ******

    /**
     * Determine if the player is currently playing MobArena
     *
     * @param player
     * @return Returns true when the player is in game.
     */
    public static boolean isPlayingMobArena(Player player) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.MobArena.getName())))
            return playersPlayingMobArena.contains(player.getUniqueId());
        return false;
    }

    /**
     * Add the player to the list of active MobArena players.
     *
     * @param player
     */
    public static void startPlayingMobArena(Player player) {
        playersPlayingMobArena.add(player.getUniqueId());
    }

    /**
     * Remove the player from list of active MobArena players
     *
     * @param player
     */
    public static void stopPlayingMobArena(Player player) {
        if (!playersPlayingMobArena.remove(player.getUniqueId())) {
            MessageHelper.debug("Player: %s is not playing MobArena", player.getName());
        }
    }
}

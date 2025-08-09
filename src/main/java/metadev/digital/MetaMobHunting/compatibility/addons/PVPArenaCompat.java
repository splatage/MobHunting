package metadev.digital.MetaMobHunting.compatibility.addons;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import metadev.digital.metacustomitemslib.compatibility.Feature;
import metadev.digital.metacustomitemslib.compatibility.FeatureList;
import metadev.digital.metacustomitemslib.compatibility.ICompat;
import metadev.digital.metacustomitemslib.compatibility.IFeatureHolder;
import metadev.digital.metacustomitemslib.compatibility.exceptions.FeatureNotFoundException;
import metadev.digital.metacustomitemslib.compatibility.exceptions.SpinupShutdownException;
import net.slipcor.pvparena.events.PADeathEvent;
import net.slipcor.pvparena.events.PAExitEvent;
import net.slipcor.pvparena.events.PAJoinEvent;
import net.slipcor.pvparena.events.PALeaveEvent;
import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.MobHunting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class PVPArenaCompat implements Listener, ICompat, IFeatureHolder {

    // ****** Standard ******
    private Plugin compatPlugin;
    private static boolean enabled = false, supported = false, loaded = false;
    private static String sMin, sMax, pMin, pMax;
    private static FeatureList features;

    // ****** Plugin Specific ******
    private static List<UUID> playersPlayingPVPArena = new ArrayList<UUID>();

	public PVPArenaCompat() {
        compatPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.PVPArena.getName());

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
        enabled = MobHunting.getInstance().getConfigManager().enableIntegrationPvpArena;
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
    private void onPvpPlayerJoin(PAJoinEvent event) {
        MessageHelper.debug("[MH]Player %s joined PVPArena: %s", event.getPlayer().getName(),
                event.getArena());
        startPlayingPVPArena(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    private void onPvpPlayerDeath(PADeathEvent event) {
        MessageHelper.debug("[MH]Player %s died in PVPArena: %s", event.getPlayer().getName(),
                event.getArena());
        // startPlayingPVPArena(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    private void onPvpPlayerLeave(PALeaveEvent event) {
        MessageHelper.debug("[MH]Player %s left PVPArena: %s", event.getPlayer().getName(),
                event.getArena());
        stopPlayingPVPArena(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    private void onPAExit(PAExitEvent event) {
        MessageHelper.debug("[MH]Player %s exit PVPArena: %s", event.getPlayer().getName(),
                event.getArena());
        stopPlayingPVPArena(event.getPlayer());
    }

    // ****** Plugin Specific ******

    /**
     * Determine if the player is currently playing PVPArena
     *
     * @param player
     * @return Return true when the player is in game.
     */
    public static boolean isPlayingPVPArena(Player player) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.PVPArena.getName())))
            return playersPlayingPVPArena.contains(player.getUniqueId());
        return false;
    }

    /**
     * Add the player to the list of active PVPArena players
     *
     * @param player
     */
    public static void startPlayingPVPArena(Player player) {
        playersPlayingPVPArena.add(player.getUniqueId());
    }

    /**
     * Remove the player from the list of active users playing PVPArena
     *
     * @param player
     */
    public static void stopPlayingPVPArena(Player player) {
        if (!playersPlayingPVPArena.remove(player.getUniqueId())) {
            MessageHelper.debug("Player: %s is not in PVPArena", player.getName());
        }
    }
}

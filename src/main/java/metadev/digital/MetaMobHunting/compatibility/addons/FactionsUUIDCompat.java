package metadev.digital.MetaMobHunting.compatibility.addons;

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
import org.bukkit.plugin.Plugin;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;

import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.MobHunting;

public class FactionsUUIDCompat implements ICompat, IFeatureHolder {

    // ****** Standard ******
    private Plugin compatPlugin;
    private static boolean enabled = false, supported = false, loaded = false;
    private static String sMin, sMax, pMin = "0.7.0", pMax;
    private static FeatureList features;

    // ****** Plugin Specific ******

	// https://www.spigotmc.org/resources/factionsuuid.1035/ https://factions.support/

	public FactionsUUIDCompat() {
        compatPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Factions.getName());

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
        enabled = MobHunting.getInstance().getConfigManager().enableIntegrationFactions;
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

    // ****** Plugin Specific ******

    public static boolean isInSafeZoneAndPeaceful(Player player) {
        FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);
        Faction faction_home = fplayer.getFaction();
        FLocation flocation = fplayer.getLastStoodAt();
        Faction faction_here = Board.getInstance().getFactionAt(flocation);
        if (faction_here != null && faction_here.isSafeZone() && !faction_here.isPeaceful()) {
            MessageHelper.debug("player is in a safe zone: %s", faction_home.getDescription());
            if (faction_here.isPeaceful()) {
                MessageHelper.debug("The safe zone is peacefull - no reward.");
                return true;
            }
            return false;
        } else
            return false;
    }

    public static boolean isInWilderness(Player player) {
        FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);
        FLocation flocation = fplayer.getLastStoodAt();
        Faction faction_here = Board.getInstance().getFactionAt(flocation);
        if (faction_here != null && faction_here.isWilderness()) {
            MessageHelper.debug("%s is in Wilderness", player.getName());
            return true;
        } else
            return false;
    }

    public static boolean isInWarZone(Player player) {
        FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);
        FLocation flocation = fplayer.getLastStoodAt();
        Faction faction_here = Board.getInstance().getFactionAt(flocation);
        if (faction_here != null && faction_here.isWarZone()) {
            MessageHelper.debug("%s is in a War zone", player.getName());
            return true;
        } else
            return false;
    }

    public static boolean isInHomeZoneAndPeaceful(Player player) {
        FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);
        Faction faction_home = fplayer.getFaction();
        FLocation flocation = fplayer.getLastStoodAt();
        Faction faction_here = Board.getInstance().getFactionAt(flocation);
        if (faction_here != null && faction_here.equals(faction_home)) {
            MessageHelper.debug("player is in home zone: %s (peaceful=%s, normal=%s) ",
                    faction_home.getDescription(), faction_here.isPeaceful(), faction_here.isNormal());
            if (faction_here.isPeaceful()) {
                MessageHelper.debug("The home zone is peacefull - no reward.");
                return true;
            }
            return false;
        } else
            return false;
    }
}

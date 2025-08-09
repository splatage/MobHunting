package metadev.digital.MetaMobHunting.compatibility;

import com.bekvon.bukkit.residence.api.ResidenceApi;
import com.bekvon.bukkit.residence.containers.ResidencePlayer;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
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

import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.MobHunting;

import java.util.List;
import java.util.Map;

public class ResidenceCompat implements ICompat, IFeatureHolder {

    // ****** Standard ******
    private Plugin compatPlugin;
    private static boolean enabled = false, supported = false, loaded = false;
    private static String sMin, sMax, pMin = "5.1.7.7", pMax;
    private static FeatureList features;

    // ****** Plugin Specific ******

	// https://www.spigotmc.org/resources/residence-1-7-10-up-to-1-11.11480/

	public ResidenceCompat() {
        compatPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Residence.getName());

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
        enabled = MobHunting.getInstance().getConfigManager().enableIntegrationResidence;
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

    public static boolean isProtected(Player player) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Residence.getName()))) {

            ResidencePlayer residencePlayer = ResidenceApi.getPlayerManager().getResidencePlayer(player.getName());

            List<ClaimedResidence> residenseList = residencePlayer.getResList();

            for (ClaimedResidence res : residenseList) {
                if (res.containsLoc(player.getLocation())) {
                    Map<String, Boolean> flags = res.getPermissions().getFlags();
                    for (String flag: flags.keySet()){
                        if (flag.equalsIgnoreCase("damage") && !flags.get(flag))
                            return true;
                    }

                }
            }
        }
        return false;
    }

}

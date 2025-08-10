package metadev.digital.MetaMobHunting.compatibility.addons;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.metacustomitemslib.compatibility.Feature;
import metadev.digital.metacustomitemslib.compatibility.FeatureList;
import metadev.digital.MetaMobHunting.compatibility.ICompat;
import metadev.digital.metacustomitemslib.compatibility.IFeatureHolder;
import metadev.digital.metacustomitemslib.compatibility.enums.BoundIdentifierEnum;
import metadev.digital.metacustomitemslib.compatibility.enums.VersionSetIdentifierEnum;
import metadev.digital.metacustomitemslib.compatibility.exceptions.FeatureNotFoundException;
import metadev.digital.metacustomitemslib.compatibility.exceptions.SpinupShutdownException;
import org.bukkit.Bukkit;

import net.citizensnpcs.api.CitizensAPI;
import metadev.digital.metabagofgold.BagOfGold;
import metadev.digital.metabagofgold.bank.BankManager;
import metadev.digital.metabagofgold.storage.DataStoreManager;
import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import org.bukkit.plugin.Plugin;

public class BagOfGoldCompat implements ICompat, IFeatureHolder {

    // ****** Standard ******
    private Plugin compatPlugin;
    private static boolean enabled = false, supported = false, loaded = false;
    private static String sMin, sMax, pMin = "4.5.7", pMax;
    private static FeatureList features;

    // ****** Plugin Specific ******

    public BagOfGoldCompat() {
        compatPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.BagOfGold.getName());

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
        enabled = MobHunting.getInstance().getConfigManager().enableIntegrationBagOfGold;
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
    public static boolean useBagOfGoldAsAnEconomyPlugin() {
        return MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.BagOfGold.getName())) && BagOfGold.getInstance().getConfigManager().useBagOfGoldAsAnEconomyPlugin;
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
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.BagOfGold.getName())))
            return CitizensAPI.getNPCRegistry().getById(id) != null;
        return false;
    }
}

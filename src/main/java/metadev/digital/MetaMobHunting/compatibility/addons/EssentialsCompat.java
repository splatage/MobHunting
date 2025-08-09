package metadev.digital.MetaMobHunting.compatibility.addons;

import java.io.File;
import java.io.IOException;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import metadev.digital.metacustomitemslib.compatibility.Feature;
import metadev.digital.metacustomitemslib.compatibility.FeatureList;
import metadev.digital.metacustomitemslib.compatibility.ICompat;
import metadev.digital.metacustomitemslib.compatibility.IFeatureHolder;
import metadev.digital.metacustomitemslib.compatibility.exceptions.FeatureNotFoundException;
import metadev.digital.metacustomitemslib.compatibility.exceptions.SpinupShutdownException;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;

import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.MobHunting;
import org.bukkit.plugin.Plugin;

public class EssentialsCompat implements ICompat, IFeatureHolder {

    // ****** Standard ******
    private static Plugin compatPlugin;
    private static boolean enabled = false, supported = false, loaded = false;
    private static String sMin, sMax, pMin, pMax;
    private static FeatureList features;

    // ****** Plugin Specific ******

	public EssentialsCompat() {
        compatPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Essentials.getName());

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
        enabled = MobHunting.getInstance().getConfigManager().enableIntegrationEssentials;
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

    // ****** Plugin Specific ******

    public static Essentials getEssentials() {
        return (Essentials) compatPlugin;
    }

    public static boolean isGodModeEnabled(Player player) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Essentials.getName()))) {
            User user = getEssentials().getUser(player);
            return user.isGodModeEnabled();
        }
        return false;
    }

    public static boolean isVanishedModeEnabled(Player player) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Essentials.getName()))) {
            User user = getEssentials().getUser(player);
            return user.isVanished();
        }
        return false;
    }

    public static double getBalance(Player player) {
        double bal = getEssentials().getOfflineUser(player.getName()).getMoney().doubleValue();
        return bal;
    }

    public static double getEssentialsBalance(OfflinePlayer offlinePlayer) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Essentials.getName()))) {
            if (EssentialsCompat.getEssentials().getOfflineUser(offlinePlayer.getName()) != null) {
                String uuid = EssentialsCompat.getEssentials().getOfflineUser(offlinePlayer.getName()).getConfigUUID()
                        .toString();
                File datafolder = EssentialsCompat.getEssentials().getDataFolder();
                if (datafolder.exists()) {
                    File configfile = new File(datafolder + "/userdata/" + uuid + ".yml");
                    if (configfile.exists()) {
                        YamlConfiguration config = new YamlConfiguration();
                        try {
                            config.load(configfile);
                        } catch (IOException | InvalidConfigurationException e) {
                            e.printStackTrace();
                            return 0;
                        }
                        return Double.valueOf(config.getString("money", "0"));
                    }
                }
            }
        }
        return 0;
    }

    public static void setEssentialsBalance(OfflinePlayer offlinePlayer, double amount) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Essentials.getName()))) {
            String uuid = EssentialsCompat.getEssentials().getOfflineUser(offlinePlayer.getName()).getConfigUUID()
                    .toString();
            File datafolder = EssentialsCompat.getEssentials().getDataFolder();
            if (datafolder.exists()) {
                File configfile = new File(datafolder + "/userdata/" + uuid + ".yml");
                if (configfile.exists()) {
                    YamlConfiguration config = new YamlConfiguration();
                    try {
                        config.load(configfile);
                        config.set("money", String.valueOf(amount));
                        config.save(configfile);
                        MessageHelper.debug("updated essentials balance to %s", amount);
                    } catch (IOException | InvalidConfigurationException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

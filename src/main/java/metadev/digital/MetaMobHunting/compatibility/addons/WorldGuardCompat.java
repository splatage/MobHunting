package metadev.digital.MetaMobHunting.compatibility.addons;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import metadev.digital.metacustomitemslib.compatibility.Feature;
import metadev.digital.metacustomitemslib.compatibility.FeatureList;
import metadev.digital.MetaMobHunting.compatibility.IMobHuntCompat;
import metadev.digital.metacustomitemslib.compatibility.IFeatureHolder;
import metadev.digital.metacustomitemslib.compatibility.enums.BoundIdentifierEnum;
import metadev.digital.metacustomitemslib.compatibility.enums.VersionSetIdentifierEnum;
import metadev.digital.metacustomitemslib.compatibility.exceptions.FeatureNotFoundException;
import metadev.digital.metacustomitemslib.compatibility.exceptions.SpinupShutdownException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;

import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.MobHunting;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WorldGuardCompat implements IMobHuntCompat, IFeatureHolder {

    // ****** Standard ******
    private static Plugin compatPlugin;
    private static boolean enabled = false, supported = false, loaded = false;
    private static String sMin, sMax, pMin = "7.0.0", pMax;
    private static FeatureList features;

    // ****** Plugin Specific ******

	public WorldGuardCompat() {
        compatPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.WorldGuard.getName());

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
            registerFlag();
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
        enabled = MobHunting.getInstance().getConfigManager().enableIntegrationWorldGuard;
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
    public static WorldGuardPlugin getWorldGuardPlugin() {
        return (WorldGuardPlugin) compatPlugin;
    }

    public static boolean isAllowedByWorldGuard(Entity damager, Entity damaged, StateFlag stateFlag, boolean defaultValue) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.WorldGuard.getName()))){
            Player checkedPlayer = null;

            if (MyPetCompat.isMyPet(damager))
                checkedPlayer = MyPetCompat.getMyPetOwner(damager);
            else if (damager instanceof Player)
                checkedPlayer = (Player) damager;

            if (checkedPlayer != null) {
                LocalPlayer localPlayer = WorldGuardCompat.getWorldGuardPlugin().wrapPlayer(checkedPlayer);
                RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
                if (container != null) {
                    // https://worldguard.enginehub.org/en/latest/developer/regions/spatial-queries/
                    RegionQuery query = container.createQuery();
                    ApplicableRegionSet set = query.getApplicableRegions(localPlayer.getLocation());
                    if (set.size() > 0) {
                        StateFlag.State flag = set.queryState(localPlayer, stateFlag);
                        if (flag != null) {
                            return flag == StateFlag.State.ALLOW;
                        }
                    }
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    public static void registerFlag() {
        //Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");
        try {
            // register MobHuting flag with the WorlsGuard Flag registry

            // wg7.x

            try {
                WorldGuard.getInstance().getFlagRegistry().register(WorldGuardMobHuntingFlag.getMobHuntingFlag());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                MessageHelper.error("Could not register MobHunting flag in WorldGuard 7.");
            }

            // wg6.x
            // ((WorldGuardPlugin)
            // wg).getFlagRegistry().register(WorldGuardHelper.getMobHuntingFlag());
        } catch (FlagConflictException e) {

            // some other plugin registered a flag by the same name already.
            // you may want to re-register with a different name, but this
            // could cause issues with saved flags in region files. it's
            // better
            // to print a message to let the server admin know of the
            // conflict
        }
    }
}

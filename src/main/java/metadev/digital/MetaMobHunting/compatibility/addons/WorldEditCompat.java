package metadev.digital.MetaMobHunting.compatibility.addons;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
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

import com.sk89q.worldedit.bukkit.WorldEditPlugin;

import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.MobHunting;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WorldEditCompat implements IMobHuntCompat, IFeatureHolder {

    // ****** Standard ******
    private static Plugin compatPlugin;
    private static boolean enabled = false, supported = false, loaded = false;
    private static String sMin, sMax, pMin = "7.0.0", pMax;
    private static FeatureList features;

    // ****** Plugin Specific ******

	public WorldEditCompat() {
        compatPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.WorldEdit.getName());

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
        enabled = MobHunting.getInstance().getConfigManager().enableIntegrationWorldEdit;
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
    public static WorldEditPlugin getWorldEdit() {
        return (WorldEditPlugin) compatPlugin;
    }


    public static BlockVector3 getPointA(Player player) throws IllegalArgumentException {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.WorldEdit.getName())))
            throw new IllegalArgumentException("WorldEdit is not present");

        com.sk89q.worldedit.world.World wor = WorldEditCompat.getWorldEdit().getSession(player).getSelectionWorld();
        Region sel = null;
        try {
            sel = WorldEditCompat.getWorldEdit().getSession(player).getSelection(wor);
        } catch (IncompleteRegionException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
        }

        if (sel == null)
            throw new IllegalArgumentException(
                    MobHunting.getInstance().getMessages().getString("mobhunting.commands.select.no-select"));

        if (!(sel instanceof CuboidRegion))
            throw new IllegalArgumentException(
                    MobHunting.getInstance().getMessages().getString("mobhunting.commands.select.select-type"));

        return sel.getMinimumPoint();
    }

    public static BlockVector3 getPointB(Player player) throws IllegalArgumentException {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.WorldEdit.getName())))
            throw new IllegalArgumentException("WorldEdit is not present");

        com.sk89q.worldedit.world.World wor = WorldEditCompat.getWorldEdit().getSession(player).getSelectionWorld();
        Region sel = null;
        try {
            sel = WorldEditCompat.getWorldEdit().getSession(player).getSelection(wor);
        } catch (IncompleteRegionException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
        }

        if (sel == null)
            throw new IllegalArgumentException(
                    MobHunting.getInstance().getMessages().getString("mobhunting.commands.select.no-select"));

        if (!(sel instanceof CuboidRegion))
            throw new IllegalArgumentException(
                    MobHunting.getInstance().getMessages().getString("mobhunting.commands.select.select-type"));

        return sel.getMaximumPoint();
    }
}

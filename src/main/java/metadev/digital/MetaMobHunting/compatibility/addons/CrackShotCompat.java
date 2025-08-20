package metadev.digital.MetaMobHunting.compatibility.addons;

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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.shampaggon.crackshot.CSUtility;
import com.shampaggon.crackshot.events.WeaponDamageEntityEvent;

import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.DamageInformation;
import metadev.digital.MetaMobHunting.MobHunting;

public class CrackShotCompat implements Listener, IMobHuntCompat, IFeatureHolder {

    // ****** Standard ******
    private Plugin compatPlugin;
    private static boolean enabled = false, supported = false, loaded = false;
    private static String sMin, sMax, pMin = "0.98.5", pMax;
    private static FeatureList features;

    // ****** Plugin Specific ******

	// https://dev.bukkit.org/projects/crackshot
	// API: https://github.com/Shampaggon/CrackShot/wiki/Hooking-into-CrackShot

	public CrackShotCompat() {
        compatPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.CrackShot.getName());

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
        enabled = MobHunting.getInstance().getConfigManager().enableIntegrationCrackShot;
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

    // ****** Listener ******
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW)
    public void onWeaponDamageEntityEvent(WeaponDamageEntityEvent event) {
        if (event.getVictim() instanceof LivingEntity) {
            DamageInformation info = MobHunting.getInstance().getMobHuntingManager().getDamageHistory()
                    .get(event.getVictim());
            if (info == null)
                info = new DamageInformation();
            info.setTime(System.currentTimeMillis());
            info.setAttacker(event.getPlayer());
            info.setAttackerPosition(event.getPlayer().getLocation().clone());
            info.setCrackShotWeapon(getCrackShotWeapon(event.getPlayer().getItemInHand()));
            info.setWeaponUser(event.getPlayer());
            MobHunting.getInstance().getMobHuntingManager().getDamageHistory().put((LivingEntity) event.getVictim(),
                    info);
        }
    }

    // ****** Plugin Specific ******
    public static boolean isCrackShotWeapon(ItemStack itemStack) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.CrackShot.getName()))) {
            CSUtility cs = new CSUtility();
            return cs.getWeaponTitle(itemStack) != null;
        }
        return false;
    }

    public static String getCrackShotWeapon(ItemStack itemStack) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.CrackShot.getName()))) {
            CSUtility cs = new CSUtility();
            return cs.getWeaponTitle(itemStack);
        }
        return null;
    }

    public static boolean isCrackShotProjectile(Projectile Projectile) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.CrackShot.getName()))) {
            CSUtility cs = new CSUtility();
            return cs.getWeaponTitle(Projectile) != null;
        }
        return false;
    }

    public static String getCrackShotWeapon(Projectile Projectile) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.CrackShot.getName()))) {
            CSUtility cs = new CSUtility();
            return cs.getWeaponTitle(Projectile);
        }
        return null;
    }

    public static boolean isCrackShotUsed(Entity entity) {
        if (MobHunting.getInstance().getMobHuntingManager().getDamageHistory().containsKey(entity))
            return MobHunting.getInstance().getMobHuntingManager().getDamageHistory().get(entity)
                    .getCrackShotWeaponUsed() != null
                    && !MobHunting.getInstance().getMobHuntingManager().getDamageHistory().get(entity)
                    .getCrackShotWeaponUsed().isEmpty();
        return false;
    }
}

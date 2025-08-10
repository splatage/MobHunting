package metadev.digital.MetaMobHunting.compatibility.addons;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import metadev.digital.metacustomitemslib.compatibility.Feature;
import metadev.digital.metacustomitemslib.compatibility.FeatureList;
import metadev.digital.MetaMobHunting.compatibility.ICompat;
import metadev.digital.metacustomitemslib.compatibility.IFeatureHolder;
import metadev.digital.metacustomitemslib.compatibility.enums.BoundIdentifierEnum;
import metadev.digital.metacustomitemslib.compatibility.enums.VersionSetIdentifierEnum;
import metadev.digital.metacustomitemslib.compatibility.exceptions.FeatureNotFoundException;
import metadev.digital.metacustomitemslib.compatibility.exceptions.SpinupShutdownException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import me.deecaad.weaponmechanics.WeaponMechanicsAPI;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponAssistEvent;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponDamageEntityEvent;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponKillEntityEvent;
import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.DamageInformation;
import metadev.digital.MetaMobHunting.MobHunting;

public class WeaponMechanicsCompat implements Listener, ICompat, IFeatureHolder {


    // ****** Standard ******
    private Plugin compatPlugin;
    private static boolean enabled = false, supported = false, loaded = false;
    private static String sMin, sMax, pMin = "1.11", pMax;
    private static FeatureList features;

    // ****** Plugin Specific ******

	public WeaponMechanicsCompat() {
        compatPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.WeaponMechanics.getName());

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
        enabled = MobHunting.getInstance().getConfigManager().enableIntegrationWeaponMechanics;
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

    @EventHandler(priority = EventPriority.LOW)
    public void onWeaponDamageEntityEvent(WeaponDamageEntityEvent event) {
        if (event.getVictim() instanceof LivingEntity) {
            DamageInformation info = MobHunting.getInstance().getMobHuntingManager().getDamageHistory()
                    .get(event.getVictim());
            if (info == null)
                info = new DamageInformation();
            info.setTime(System.currentTimeMillis());
            info.setAttacker((Player) event.getShooter());
            info.setAttackerPosition(event.getShooter().getLocation().clone());
            info.setCrackShotWeapon(getWeaponMechanicsWeapon(((Player) event.getShooter()).getItemInHand()));
            info.setWeaponUser((Player) event.getShooter());
            MobHunting.getInstance().getMobHuntingManager().getDamageHistory().put(event.getVictim(), info);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onWeaponKillEntityEvent(WeaponKillEntityEvent event) {
        // TESTING
        LivingEntity victim = event.getVictim();
        Player player = (Player) event.getShooter();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onWeaponAssistEvent(WeaponAssistEvent event) {
        // TESTING
        LivingEntity victim = event.getKilled();
        // Player player = (Player) event.getAssistInfo().;
    }

    // ****** Plugin Specific ******

    public static String getWeaponMechanicsWeapon(ItemStack itemStack) {
        return WeaponMechanicsAPI.getWeaponTitle(itemStack);
    }

    public static boolean isWeaponMechanicsWeapon(ItemStack itemStack) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.WeaponMechanics.getName()))) {
            String weaponName = WeaponMechanicsAPI.getWeaponTitle(itemStack);
            if (weaponName != null) {
                Core.getMessages().debug("This is a WeaponMechanics weapon: %s", weaponName);
                return true;
            }
        }
        return false;
    }

    public static boolean isWeaponMechanicsWeaponUsed(Entity entity) {
        if (MobHunting.getInstance().getMobHuntingManager().getDamageHistory().containsKey(entity))
            return MobHunting.getInstance().getMobHuntingManager().getDamageHistory().get(entity)
                    .getCrackShotWeaponUsed() != null
                    && !MobHunting.getInstance().getMobHuntingManager().getDamageHistory().get(entity)
                    .getWeaponMechanicsWeapon().isEmpty();
        return false;
    }
}

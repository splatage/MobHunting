package metadev.digital.MetaMobHunting.compatibility;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import metadev.digital.metacustomitemslib.compatibility.Feature;
import metadev.digital.metacustomitemslib.compatibility.FeatureList;
import metadev.digital.metacustomitemslib.compatibility.ICompat;
import metadev.digital.metacustomitemslib.compatibility.IFeatureHolder;
import metadev.digital.metacustomitemslib.compatibility.exceptions.FeatureNotFoundException;
import metadev.digital.metacustomitemslib.compatibility.exceptions.SpinupShutdownException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.event.MyPetInventoryActionEvent;
import de.Keyle.MyPet.api.event.MyPetInventoryActionEvent.Action;
import de.Keyle.MyPet.api.event.MyPetPickupItemEvent;
import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.metacustomitemslib.rewards.Reward;
import metadev.digital.MetaMobHunting.MobHunting;
import org.bukkit.plugin.Plugin;

public class MyPetCompat implements Listener, ICompat, IFeatureHolder {


    // ****** Standard ******
    private static Plugin compatPlugin;
    private static boolean enabled = false, supported = false, loaded = false;
    private static String sMin, sMax, pMin, pMax;
    private static FeatureList features;

    // ****** Plugin Specific ******

	public MyPetCompat() {
        compatPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.MyPet.getName());

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
        enabled = MobHunting.getInstance().getConfigManager().enableIntegrationMyPet;
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onMyPetKillMob(EntityDeathEvent event) {
        // MobHunting is not started initialized yet...
        if (MobHunting.getInstance().getMobHuntingManager() == null)
            return;

        if (!MobHunting.getInstance().getMobHuntingManager().isHuntEnabledInWorld(event.getEntity().getWorld())
                || !(event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent))
            return;

        EntityDamageByEntityEvent dmg = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();
        if (dmg == null || !(dmg.getDamager() instanceof MyPetBukkitEntity))
            return;

        MyPetBukkitEntity killer = (MyPetBukkitEntity) dmg.getDamager();
        if (killer.getOwner() != null) {
            Player owner = killer.getOwner().getPlayer();
            if (owner != null && MobHunting.getInstance().getMobHuntingManager().isHuntEnabled(owner))
                MobHunting.getInstance().getAchievementManager().awardAchievementProgress("fangmaster", owner,
                        MobHunting.getInstance().getExtendedMobManager().getExtendedMobFromEntity(event.getEntity()),
                        1);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    private void onMyPetInventoryActionEvent(MyPetInventoryActionEvent event) {
        if (event.getAction() == Action.Pickup)
            MessageHelper.debug("MyPetInventoryActionEvent=%s", event.getAction().name());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    private void onMyPetPickupItem(MyPetPickupItemEvent event) {
        if (event.isCancelled())
            return;

        Item item = event.getItem();
        Player player = event.getOwner().getPlayer();
        MyPet pet = event.getPet();

        if (Reward.isReward(item)) {
            Reward reward = Reward.getReward(item);
            MobHunting.getInstance().getMessages().playerActionBarMessageQueue(player,
                    MobHunting.getInstance().getMessages().getString("mobhunting.reward.mypet_pickup", "rewardname",
                            ChatColor.valueOf(Core.getConfigManager().rewardTextColor) + reward.getDisplayName(),
                            "petname", pet.getPetName(), "money",
                            MobHunting.getInstance().getEconomyManager().format(reward.getMoney())));
            MessageHelper.debug("%s owned by %s picked up %s %s.", pet.getPetName(),
                    player.getName(), MobHunting.getInstance().getEconomyManager().format(reward.getMoney()),
                    reward.getDisplayName());
            if (reward.isBagOfGoldReward() || reward.isItemReward()) {
                if (!MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.BagOfGold.getName()))
                        && !MobHunting.getInstance().getConfigManager().dropMoneyOnGroundUseItemAsCurrency) {
                    event.setCancelled(true);
                    item.remove();
                    MobHunting.getInstance().getRewardManager().depositPlayer(player, reward.getMoney());
                }
            }
        }
    }

    // ****** Plugin Specific ******
    public static MyPetPlugin getMyPetPlugin() {
        return (MyPetPlugin) compatPlugin;
    }

    public static boolean isMyPet(Entity entity) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.MyPet.getName())))
            return entity instanceof MyPetBukkitEntity;
        return false;
    }

    public static boolean isKilledByMyPet(Entity entity) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.MyPet.getName())) && (entity.getLastDamageCause() instanceof EntityDamageByEntityEvent)) {
            EntityDamageByEntityEvent dmg = (EntityDamageByEntityEvent) entity.getLastDamageCause();
            if (dmg != null && (dmg.getDamager() instanceof MyPetBukkitEntity))
                return true;
        }
        return false;
    }

    public static MyPetBukkitEntity getMyPet(Entity entity) {
        EntityDamageByEntityEvent dmg = (EntityDamageByEntityEvent) entity.getLastDamageCause();

        if (dmg == null || !(dmg.getDamager() instanceof MyPetBukkitEntity))
            return null;

        MyPetBukkitEntity killer = (MyPetBukkitEntity) dmg.getDamager();

        return killer;
    }

    public static Player getMyPetOwner(Entity entity) {

        if (!(entity.getLastDamageCause() instanceof EntityDamageByEntityEvent))
            return null;

        EntityDamageByEntityEvent dmg = (EntityDamageByEntityEvent) entity.getLastDamageCause();

        if (dmg == null || !(dmg.getDamager() instanceof MyPetBukkitEntity))
            return null;

        MyPetBukkitEntity killer = (MyPetBukkitEntity) dmg.getDamager();

        if (killer.getOwner() == null)
            return null;

        return killer.getOwner().getPlayer();
    }
}

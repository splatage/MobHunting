package metadev.digital.MetaMobHunting.compatibility.addons;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import metadev.digital.MetaMobHunting.mobs.MobPlugin;
import metadev.digital.metacustomitemslib.compatibility.Feature;
import metadev.digital.metacustomitemslib.compatibility.FeatureList;
import metadev.digital.MetaMobHunting.compatibility.IMobHuntCompat;
import metadev.digital.metacustomitemslib.compatibility.IFeatureHolder;
import metadev.digital.metacustomitemslib.compatibility.enums.BoundIdentifierEnum;
import metadev.digital.metacustomitemslib.compatibility.enums.VersionSetIdentifierEnum;
import metadev.digital.metacustomitemslib.compatibility.exceptions.FeatureNotFoundException;
import metadev.digital.metacustomitemslib.compatibility.exceptions.SpinupShutdownException;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.StatType;
import metadev.digital.MetaMobHunting.mobs.ExtendedMobRewardData;

public class MythicMobsCompat implements Listener, IMobHuntCompat, IFeatureHolder {

    // ****** Standard ******
    private static Plugin compatPlugin;
    private static boolean enabled = false, supported = false, loaded = false;
    private static String sMin, sMax, pMin = "5.0.0", pMax;
    private static FeatureList features;

    // ****** Plugin Specific ******
	private static HashMap<String, ExtendedMobRewardData> mMobRewardData = new HashMap<String, ExtendedMobRewardData>();
	private static File file = new File(MobHunting.getInstance().getDataFolder(), "mythicmobs-rewards.yml");
	private static YamlConfiguration config = new YamlConfiguration();

	public static final String MH_MYTHICMOBS = "MH:MYTHICMOBS";

	public MythicMobsCompat() {
        compatPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.MythicMobs.getName());

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
            MythicMobsCompat.loadMythicMobsData();
            MythicMobsCompat.saveMythicMobsData();
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
        enabled = MobHunting.getInstance().getConfigManager().enableIntegrationMythicmobs;
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

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onMythicMobSpawnEvent(MythicMobSpawnEvent event) {
        String mobtype = event.getMobType().getInternalName();

        if (!MythicMobsCompat.getMobRewardData().containsKey(mobtype)) {
            MessageHelper.debug("New MythicMobType found=%s", mobtype);
            MythicMobsCompat.getMobRewardData().put(mobtype,
                    new ExtendedMobRewardData(MobPlugin.MythicMobs, mobtype, mobtype, true, "10", 1,
                            "You killed a MythicMob", new ArrayList<HashMap<String, String>>(), 1, 0.02));
            MythicMobsCompat.saveMythicMobsData(mobtype);
            MobHunting.getInstance().getStoreManager().insertMissingMythicMobs(mobtype);

            // Update mob loaded into memory
            MobHunting.getInstance().getMessages().injectMissingMobNamesToLangFiles();
            MobHunting.getInstance().getExtendedMobManager().updateExtendedMobs();
        }

        event.getEntity().setMetadata(MythicMobsCompat.MH_MYTHICMOBS,
                new FixedMetadataValue(compatPlugin, MythicMobsCompat.getMobRewardData().get(mobtype)));
    }

    @SuppressWarnings("unused")
    private void onMythicMobDeathEvent(MythicMobDeathEvent event) {

    }

    // ****** Plugin Specific ******
    public static void loadMythicMobsData() {
        try {
            if (!file.exists())
                return;

            MessageHelper.debug("Loading extra MobRewards for MythicMobs mobs.");

            config.load(file);
            int n = 0;
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (isMythicMob(key)) {
                    ExtendedMobRewardData mob = new ExtendedMobRewardData();
                    mob.read(section);
                    mob.setMobType(key);
                    mMobRewardData.put(key, mob);
                    MobHunting.getInstance().getStoreManager().insertMissingMythicMobs(key);
                    n++;
                } else {
                    MessageHelper.debug("The mob=%s can't be found in MythicMobs configuration files", key);
                }
            }
            MessageHelper.debug("Loaded %s MythicMobs", n);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }

    }

    public static void loadMythicMobsData(String key) {
        try {
            if (!file.exists())
                return;

            config.load(file);
            ConfigurationSection section = config.getConfigurationSection(key);
            if (isMythicMob(key)) {
                ExtendedMobRewardData mob = new ExtendedMobRewardData();
                mob.read(section);
                mob.setMobType(key);
                mMobRewardData.put(key, mob);
                int n = StatType.values().length;
                StatType.values()[n + 1] = new StatType(mob.getMobType() + "_kill", mob.getMobName());
                StatType.values()[n + 2] = new StatType(mob.getMobType() + "_assist", mob.getMobName());
                MobHunting.getInstance().getStoreManager().insertMissingMythicMobs(key);
            } else {
                MessageHelper.debug("The mob=%s can't be found in MythicMobs configuration files", key);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static void saveMythicMobsData() {
        try {
            config.options().header("This a extra MobHunting config data for the MythicMobs on your server.");

            if (mMobRewardData.size() > 0) {

                int n = 0;
                for (String str : mMobRewardData.keySet()) {
                    ConfigurationSection section = config.createSection(str);
                    mMobRewardData.get(str).save(section);
                    n++;
                }

                if (n != 0) {
                    MessageHelper.debug("Saving Mobhunting extra MythicMobs data.");
                    config.save(file);
                }
            } else {
                MessageHelper.debug("No Mythicmobs");
            }
        } catch (IOException e) {
            MessageHelper.debug("Could not save extra MythicMobs data.");
            e.printStackTrace();
        }
    }

    public static void saveMythicMobsData(String key) {
        try {
            if (mMobRewardData.containsKey(key)) {
                ConfigurationSection section = config.createSection(key);
                mMobRewardData.get(key).save(section);
                MessageHelper.debug("Saving Mobhunting extra MythicMobs data.");
                config.save(file);
            } else {
                MessageHelper.debug("ERROR! MythicMobs ID (%s) is not found in mMobRewardData",
                        key);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<String, ExtendedMobRewardData> getMobRewardData() {
        return mMobRewardData;
    }

    public static boolean isMythicMob(String mob) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.MythicMobs.getName())))
            return getMythicMob(mob) != null;
        return false;
    }

    public static String getMythicMobName(String mob) {
        if(getMythicMob(mob).getInternalName() != null){
            return getMythicMob(mob).getInternalName();
        }
        return "Unknown";
    }

    public static boolean isMythicMob(Entity killed) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.MythicMobs.getName())))
            return killed.hasMetadata(MH_MYTHICMOBS);
        return false;
    }

    public static String getMythicMobType(Entity killed) {
        List<MetadataValue> data = killed.getMetadata(MythicMobsCompat.MH_MYTHICMOBS);
        for (MetadataValue mdv : data) {
            if (mdv.value() instanceof ExtendedMobRewardData)
                return ((ExtendedMobRewardData) mdv.value()).getMobType();
        }
        return null;
    }

    public static int getProgressAchievementLevel1(String mobtype) {
        return mMobRewardData.get(mobtype).getAchivementLevel1();
    }

    private static MythicBukkit getMythicMobs() {
        return (MythicBukkit) compatPlugin;
    }

    public static MythicMob getMythicMob(String killed) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.MythicMobs.getName())))
            return getMythicMobs().getAPIHelper().getMythicMob(killed);
        return null;
    }
}

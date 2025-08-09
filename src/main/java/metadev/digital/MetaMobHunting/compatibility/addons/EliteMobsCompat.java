package metadev.digital.MetaMobHunting.compatibility.addons;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import com.magmaguy.elitemobs.EliteMobs;
import com.magmaguy.elitemobs.MetadataHandler;
import com.magmaguy.elitemobs.entitytracker.EntityTracker;
import com.magmaguy.elitemobs.mobconstructor.EliteEntity;

import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.mobs.MobPlugin;
import metadev.digital.MetaMobHunting.mobs.ExtendedMobRewardData;

// TODO: Some of this functionality was originally written for a version of EliteMobs ~~~IN 2014~~~. Audit and improve.
public class EliteMobsCompat implements Listener, ICompat, IFeatureHolder {

	// https://www.spigotmc.org/resources/%E2%9A%94elitemobs%E2%9A%94.40090/

    // ****** Standard ******
    private Plugin compatPlugin;
    private static boolean enabled = false, supported = false, loaded = false;
    private static String sMin, sMax, pMin = "6.5.0", pMax;
    private static FeatureList features;

    // ****** Plugin Specific ******
	private static HashMap<String, ExtendedMobRewardData> mMobRewardData = new HashMap<String, ExtendedMobRewardData>();
	private static File file = new File(MobHunting.getInstance().getDataFolder(), "EliteMobs-rewards.yml");
	private static YamlConfiguration config = new YamlConfiguration();
	public static final String MH_ELITEMOBS = "MH:ELITEMOBS";

	public EliteMobsCompat() {
        compatPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.EliteMobs.getName());

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
            loadEliteMobsMobsData();
            saveEliteMobsData();
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
        enabled = MobHunting.getInstance().getConfigManager().enableIntegrationEliteMobs;
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
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onEliteMobsSpawnEvent(EntitySpawnEvent event) {

        Entity entity = event.getEntity();

        if (isEliteMobs(entity)) {

            Mobs monster = getEliteMobsType(entity);

            if (mMobRewardData != null && !mMobRewardData.containsKey(monster.name())) {
                MessageHelper.debug("New EliteMob found=%s", monster.name());
                mMobRewardData.put(monster.name(),
                        new ExtendedMobRewardData(MobPlugin.EliteMobs, monster.name(), monster.getName(), true, "40:60",
                                1, "You killed an EliteMob", new ArrayList<HashMap<String, String>>(), 1, 0.02));
                saveEliteMobsData(monster.name());
                MobHunting.getInstance().getStoreManager().insertEliteMobs(monster.name());
                // Update mob loaded into memory
                MobHunting.getInstance().getExtendedMobManager().updateExtendedMobs();
                MobHunting.getInstance().getMessages().injectMissingMobNamesToLangFiles();
            }

            event.getEntity().setMetadata(MH_ELITEMOBS,
                    new FixedMetadataValue(compatPlugin, mMobRewardData.get(monster.name())));
        }
    }

    // ****** Plugin Specific ******
    public static void loadEliteMobsMobsData() {
        try {
            if (!file.exists()) {
                for (Mobs monster : Mobs.values()) {
                    mMobRewardData.put(monster.name(),
                            new ExtendedMobRewardData(MobPlugin.EliteMobs, monster.name(), monster.getName(), true,
                                    "10:20", 1, "You killed an EliteMob", new ArrayList<HashMap<String, String>>(), 1,
                                    0.02));
                    saveEliteMobsData(monster.name());
                    MobHunting.getInstance().getStoreManager().insertEliteMobs(monster.name());
                }
                // MobHunting.getInstance().getMessages().injectMissingMobNamesToLangFiles();
                return;
            }

            config.load(file);
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                ExtendedMobRewardData mob = new ExtendedMobRewardData();
                mob.read(section);
                mob.setMobType(key);
                mMobRewardData.put(key, mob);
                MobHunting.getInstance().getStoreManager().insertEliteMobs(key);
            }
            MessageHelper.debug("Loaded %s EliteMobs", mMobRewardData.size());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }

    }

    public static void loadEliteMobsData(String key) {
        try {
            if (!file.exists()) {
                return;
            }

            config.load(file);
            ConfigurationSection section = config.getConfigurationSection(key);
            ExtendedMobRewardData mob = new ExtendedMobRewardData();
            mob.read(section);
            mob.setMobType(key);
            mMobRewardData.put(key, mob);
            MobHunting.getInstance().getStoreManager().insertEliteMobs(key);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static void saveEliteMobsData() {
        try {
            config.options().header("This a extra MobHunting config data for the EliteMobs on your server.");

            if (mMobRewardData.size() > 0) {

                int n = 0;
                for (String str : mMobRewardData.keySet()) {
                    ConfigurationSection section = config.createSection(str);
                    mMobRewardData.get(str).save(section);
                    n++;
                }

                if (n != 0) {
                    MessageHelper.debug("Saving Mobhunting extra EliteMobs data.");
                    config.save(file);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveEliteMobsData(String key) {
        try {
            if (mMobRewardData.containsKey(key)) {
                ConfigurationSection section = config.createSection(key);
                mMobRewardData.get(key).save(section);
                MessageHelper.debug("Saving extra EliteMobs data for mob=%s (%s)", key,
                        mMobRewardData.get(key).getMobName());
                config.save(file);
            } else {
                MessageHelper.debug("ERROR! EliteMobs ID (%s) is not found in mMobRewardData",
                        key);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public EliteMobs getEliteMobs() {
        return (EliteMobs) compatPlugin;
    }

    public static boolean isEliteMobs(Entity entity) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.EliteMobs.getName())))
            return EntityTracker.isEliteMob(entity);
        return false;
    }

    public static enum Mobs {
        Custom(MetadataHandler.ELITE_MOBS);

        private String name;

        private Mobs(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    };

    public static Mobs getEliteMobsType(Entity entity) {
        return Mobs.Custom;
    }

    public static String getName(Entity entity) {
        return MobHunting.getInstance().getMessages().getString("mobs.EliteMobs.elitemob");
    }

    public static int getEliteMobsLevel(Entity entity) {
        if (isEliteMobs(entity)){
            EliteEntity eliteEntity = EntityTracker.getEliteMobEntity(entity);
            return eliteEntity != null ? eliteEntity.getLevel() : 1;
        }
        return 0;
    }

    public static HashMap<String, ExtendedMobRewardData> getMobRewardData() {
        return mMobRewardData;
    }

    public static int getProgressAchievementLevel1(String mobtype) {
        return mMobRewardData.get(mobtype).getAchivementLevel1();
    }

}


package metadev.digital.MetaMobHunting.compatibility.addons;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import metadev.digital.MetaMobHunting.compatibility.IMobHuntCompat;
import metadev.digital.metacustomitemslib.compatibility.Feature;
import metadev.digital.metacustomitemslib.compatibility.FeatureList;
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

import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.mobs.MobPlugin;
import metadev.digital.MetaMobHunting.mobs.ExtendedMobRewardData;
import de.fof1092.MysteriousHalloweenAPI;
import de.fof1092.MysteriousHalloweenAPI.MobType;

public class MysteriousHalloweenCompat implements Listener, IMobHuntCompat, IFeatureHolder {

    // ****** Standard ******
    private Plugin compatPlugin;
    private static boolean enabled = false, supported = false, loaded = false;
    private static String sMin, sMax, pMin = "4.8.4", pMax;
    private static FeatureList features;

    // ****** Plugin Specific ******
	private static HashMap<String, ExtendedMobRewardData> mMobRewardData = new HashMap<String, ExtendedMobRewardData>();
	private static File file = new File(MobHunting.getInstance().getDataFolder(), "MysteriousHalloween-rewards.yml");
	private static YamlConfiguration config = new YamlConfiguration();
	public static final String MH_MYSTERIOUSHALLOWEEN = "MH:MysteriousHalloween";

	// https://www.spigotmc.org/resources/mysterioushalloween.13059/

	public MysteriousHalloweenCompat() {
        compatPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.MysteriousHalloween.getName());

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
            loadMysteriousHalloweenMobsData();
            saveMysteriousHalloweenMobsData();
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
        enabled = MobHunting.getInstance().getConfigManager().enableIntegrationMysteriousHalloween;
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
    private void onMysteriousHalloweenSpawnEvent(EntitySpawnEvent event) {

        Entity entity = event.getEntity();

        if (isMysteriousHalloween(entity)) {

            MobType monster = getMysteriousHalloweenType(entity);

            if (mMobRewardData != null && !mMobRewardData.containsKey(monster.name())) {
                MessageHelper.debug("New MysteriousHalloween mob found=%s (%s)",
                        monster.name(), monster.toString());
                mMobRewardData.put(monster.name(), new ExtendedMobRewardData(MobPlugin.MysteriousHalloween,
                        monster.name(), MysteriousHalloweenAPI.getMobTypeName(monster), true, "40:60", 1,
                        "You killed a MysteriousHalloween mob", new ArrayList<HashMap<String, String>>(), 1, 0.02));
                saveMysteriousHalloweenMobsData(monster.name());
                MobHunting.getInstance().getStoreManager().insertMysteriousHalloweenMobs(monster.name());
                // Update mob loaded into memory
                MobHunting.getInstance().getExtendedMobManager().updateExtendedMobs();
                MobHunting.getInstance().getMessages().injectMissingMobNamesToLangFiles();
            }

            event.getEntity().setMetadata(MH_MYSTERIOUSHALLOWEEN,
                    new FixedMetadataValue(compatPlugin, mMobRewardData.get(monster.name())));
        }
    }

    public static int getProgressAchievementLevel1(String mobtype) {
        return mMobRewardData.get(mobtype).getAchivementLevel1();
    }

    // ****** Plugin Specific ******


    /**
     * Returns whether an entity is a MysteriousHalloween entity.
     *
     * @param entity the entity to check
     * @return true if the entity is a MysteriousHalloween entity
     */

    public static boolean isMysteriousHalloween(Entity entity) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.MysteriousHalloween.getName())))
            return MysteriousHalloweenAPI.isEntity(entity);
        return false;
    }

    /**
     * Returns the Monster type for a MysteriousHalloween entity.
     *
     * @param entity the entity to get the mob type for
     * @return the mob type or null if it is not MysteriousHalloween entity
     */

    public static MobType getMysteriousHalloweenType(Entity entity) {
        if (isMysteriousHalloween(entity))
            return MysteriousHalloweenAPI.getMobType(entity);
        return null;
    }

    public static HashMap<String, ExtendedMobRewardData> getMobRewardData() {
        return mMobRewardData;
    }

    public static void loadMysteriousHalloweenMobsData() {
        try {
            if (!file.exists()) {
                for (MobType monster : MysteriousHalloweenAPI.getMobTypes()) {
                    mMobRewardData.put(monster.name(), new ExtendedMobRewardData(MobPlugin.MysteriousHalloween,
                            monster.name(), MysteriousHalloweenAPI.getMobTypeName(monster), true, "40:60", 1,
                            "You killed a MysteriousHalloween mob", new ArrayList<HashMap<String, String>>(), 1, 0.02));
                    saveMysteriousHalloweenMobsData(mMobRewardData.get(monster.name()).getMobType());
                }
                return;
            }

            config.load(file);
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                ExtendedMobRewardData mob = new ExtendedMobRewardData();
                mob.read(section);
                mob.setMobType(key);
                mMobRewardData.put(key, mob);
                MobHunting.getInstance().getStoreManager().insertMysteriousHalloweenMobs(key);
            }
            MessageHelper.debug("Loaded %s MysteriousHalloween-Mobs", mMobRewardData.size());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }

    }

    public static void loadMysteriousHalloweenMobsData(String key) {
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
            MobHunting.getInstance().getStoreManager().insertMysteriousHalloweenMobs(key);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static void saveMysteriousHalloweenMobsData() {
        try {
            config.options().header("This a extra MobHunting config data for the MysteriousHalloween on your server.");

            if (mMobRewardData.size() > 0) {

                int n = 0;
                for (String str : mMobRewardData.keySet()) {
                    ConfigurationSection section = config.createSection(str);
                    mMobRewardData.get(str).save(section);
                    n++;
                }

                if (n != 0) {
                    MessageHelper.debug("Saving Mobhunting extra MysteriousHalloween data.");
                    config.save(file);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveMysteriousHalloweenMobsData(String key) {
        try {
            if (mMobRewardData.containsKey(key)) {
                ConfigurationSection section = config.createSection(key);
                mMobRewardData.get(key).save(section);
                MessageHelper.debug("Saving extra MysteriousHalloweens data for mob=%s (%s)",
                        key, mMobRewardData.get(key).getMobName());
                config.save(file);
            } else {
                MessageHelper.debug("ERROR! MysteriousHalloween ID (%s) is not found in mMobRewardData", key);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

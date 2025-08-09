package metadev.digital.MetaMobHunting.compatibility;

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
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.CitizensPlugin;
import net.citizensnpcs.api.event.CitizensEnableEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.TraitInfo;
import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.commands.NpcCommand;
import metadev.digital.MetaMobHunting.mobs.MobPlugin;
import metadev.digital.MetaMobHunting.mobs.ExtendedMobRewardData;
import metadev.digital.MetaMobHunting.npc.MasterMobHunter;
import metadev.digital.MetaMobHunting.npc.MasterMobHunterEvents;
import metadev.digital.MetaMobHunting.npc.MasterMobHunterManager;
import metadev.digital.MetaMobHunting.npc.MasterMobHunterTrait;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

public class CitizensCompat implements Listener, ICompat, IFeatureHolder {

    // ****** Standard ******
    private Plugin compatPlugin;
    private static boolean enabled = false, supported = false, loaded = false;
    private static String sMin, sMax, pMin = "2.0.39", pMax;
    private static FeatureList features;

    // ****** Plugin Specific ******
	private static CitizensPlugin citizensAPI;
	private static HashMap<String, ExtendedMobRewardData> mMobRewardData = new HashMap<String, ExtendedMobRewardData>();
	private static MasterMobHunterManager mMasterMobHunterManager;
    private static TraitInfo trait;
	private static File fileMobRewardData = new File(MobHunting.getInstance().getDataFolder(), "citizens-rewards.yml");
	private static YamlConfiguration config = new YamlConfiguration();
	public static final String MH_CITIZENS = "MH:CITIZENS";

	public CitizensCompat() {
        compatPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Citizens.getName());

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
            citizensAPI = (CitizensPlugin) compatPlugin;
            trait = TraitInfo.create(MasterMobHunterTrait.class).withName("MasterMobHunter");
            citizensAPI.getTraitFactory().registerTrait(trait);

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
            citizensAPI.getTraitFactory().deregisterTrait(trait);
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
        enabled = MobHunting.getInstance().getConfigManager().enableIntegrationCitizens;
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
    private void onCitizensEnableEvent(CitizensEnableEvent event) {

        loadCitizensData();
        mMasterMobHunterManager = new MasterMobHunterManager(MobHunting.getInstance());

        int counter = 0;
        NPCRegistry n = CitizensAPI.getNPCRegistry();
        for (NPC npc : n) {
            if (isSentryOrSentinelOrSentries(npc.getEntity())) {
                if (mMobRewardData != null && !mMobRewardData.containsKey(String.valueOf(npc.getId()))) {
                    MessageHelper.debug("A new Sentinel or Sentry NPC was found. ID=%s,%s",
                            npc.getId(), npc.getName());
                    mMobRewardData.put(String.valueOf(npc.getId()),
                            new ExtendedMobRewardData(MobPlugin.Citizens, "npc", npc.getFullName(), true, "10", 1,
                                    "You killed a Citizen", new ArrayList<HashMap<String, String>>(), 1, 0.02));
                    saveCitizensData(String.valueOf(npc.getId()));
                }
            }
            if (CitizensCompat.getMasterMobHunterManager().isMasterMobHunter(npc.getEntity())) {
                if (!CitizensCompat.getMasterMobHunterManager().contains(npc.getId())) {
                    MasterMobHunter masterMobHunter = new MasterMobHunter(MobHunting.getInstance(), npc);
                    CitizensCompat.getMasterMobHunterManager().put(npc.getId(), masterMobHunter);
                    ExtendedMobRewardData rewardData = new ExtendedMobRewardData(MobPlugin.Citizens, "npc",
                            npc.getFullName(), true, "0", 1, "You killed a Citizen",
                            new ArrayList<HashMap<String, String>>(), 1, 0.02);
                    CitizensCompat.getMobRewardData().put(String.valueOf(npc.getId()), rewardData);
                    npc.getEntity().setMetadata(CitizensCompat.MH_CITIZENS,
                            new FixedMetadataValue(MobHunting.getInstance(), rewardData));
                    MobHunting.getInstance().getStoreManager().insertCitizensMobs(String.valueOf(npc.getId()));
                    counter++;
                }
            }
        }
        if (counter > 0) {
            MobHunting.getInstance().getExtendedMobManager().updateExtendedMobs();
            MobHunting.getInstance().getMessages().injectMissingMobNamesToLangFiles();
        }

        Bukkit.getPluginManager().registerEvents(new MasterMobHunterEvents(), MobHunting.getInstance());

        MobHunting.getInstance().getCommandDispatcher().registerCommand(new NpcCommand(MobHunting.getInstance()));

        saveCitizensData();
    }

    // ****** Plugin Specific ******
    public static void loadCitizensData() {
        try {
            if (!fileMobRewardData.exists())
                return;

            config.load(fileMobRewardData);
            int n = 0;
            for (String key : config.getKeys(false)) {
                if (isNPC(Integer.valueOf(key))) {
                    ConfigurationSection section = config.getConfigurationSection(key);
                    ExtendedMobRewardData rewardData = new ExtendedMobRewardData();
                    rewardData.read(section);
                    if (mMobRewardData.get(key) == null || mMobRewardData.get(key).getMobName().equals(""))
                        rewardData.setMobName("Unknown");
                    mMobRewardData.put(key, rewardData);
                    MobHunting.getInstance().getStoreManager().insertCitizensMobs(key);
                    n++;
                } else {
                    MessageHelper.debug("The mob=%s can't be found in Citizens saves.yml file",
                            key);
                }
            }
            if (n > 0)
                MessageHelper.debug("Loaded %s MobRewards Citizens2.", n);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }

    }

    public static void saveCitizensData() {
        try {
            config.options().header("This a extra MobHunting config data for the Citizens/NPC's on your server.");

            if (mMobRewardData.size() > 0) {

                int n = 0;
                for (String key : mMobRewardData.keySet()) {
                    ConfigurationSection section = config.createSection(key);
                    mMobRewardData.get(key).save(section);
                    n++;
                }

                if (n > 0) {
                    MessageHelper.debug("Saving %s MobRewards for Citizens2 to file.",
                            mMobRewardData.size());
                    config.save(fileMobRewardData);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveCitizensData(String key) {
        try {
            if (mMobRewardData.containsKey(key)) {
                ConfigurationSection section = config.createSection(key);
                mMobRewardData.get(key).save(section);
                MessageHelper.debug("Saving MobRewardData for Citizens2: ID=%s.", key);
                config.save(fileMobRewardData);
            } else {
                MessageHelper.debug("ERROR! Sentry/Sentinel ID (%s) is not found in mMobRewardData", key);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static CitizensPlugin getCitizensPlugin() {
        return citizensAPI;
    }

    public static boolean isNPC(Entity entity) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Citizens.getName())) && CitizensAPI.hasImplementation())
            return CitizensAPI.getNPCRegistry().isNPC(entity);
        return false;
    }

    public static boolean isNPC(Integer id) {
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Citizens.getName())))
            return CitizensAPI.getNPCRegistry().getById(id) != null;
        return false;
    }

    public static int getNPCId(Entity entity) {
        return CitizensAPI.getNPCRegistry().getNPC(entity).getId();
    }

    public static String getNPCName(Entity entity) {
        String name = CitizensAPI.getNPCRegistry().getNPC(entity).getFullName();
        if (name.equals(""))
            name = String.valueOf(CitizensAPI.getNPCRegistry().getNPC(entity).getId());
        return name;
    }

    public static NPC getNPC(Entity entity) {
        return CitizensAPI.getNPCRegistry().getNPC(entity);
    }

    public static boolean isSentryOrSentinelOrSentries(Entity entity) {
        if (isNPC(entity))
            return CitizensAPI.getNPCRegistry().getNPC(entity)
                    .hasTrait(CitizensAPI.getTraitFactory().getTraitClass("Sentry"))
                    || CitizensAPI.getNPCRegistry().getNPC(entity)
                    .hasTrait(CitizensAPI.getTraitFactory().getTraitClass("Sentinel"))
                    || CitizensAPI.getNPCRegistry().getNPC(entity)
                    .hasTrait(CitizensAPI.getTraitFactory().getTraitClass("Sentries"));
        return false;
    }

    public static boolean isSentryOrSentinelOrSentries(String mobtype) {
        if (CitizensCompat.isNPC(Integer.valueOf(mobtype)))
            return CitizensAPI.getNPCRegistry().getById(Integer.valueOf(mobtype))
                    .hasTrait(CitizensAPI.getTraitFactory().getTraitClass("Sentry"))
                    || CitizensAPI.getNPCRegistry().getById(Integer.valueOf(mobtype))
                    .hasTrait(CitizensAPI.getTraitFactory().getTraitClass("Sentinel"))
                    || CitizensAPI.getNPCRegistry().getById(Integer.valueOf(mobtype))
                    .hasTrait(CitizensAPI.getTraitFactory().getTraitClass("Sentries"));
        else
            return false;
    }

    public static HashMap<String, ExtendedMobRewardData> getMobRewardData() {
        return mMobRewardData;
    }

    public static int getProgressAchievementLevel1(String mobtype) {
        return mMobRewardData.get(mobtype).getAchivementLevel1();
    }

    /**
     * Get the MasterMobHunterManager
     *
     * @return
     */
    public static MasterMobHunterManager getMasterMobHunterManager() {
        return mMasterMobHunterManager;
    }

    public void setSkin(Integer id) {
        // CitizensAPI.
    }
}
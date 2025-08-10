package metadev.digital.MetaMobHunting;

import java.io.File;
import java.util.Random;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import metadev.digital.MetaMobHunting.Messages.Messages;
import metadev.digital.MetaMobHunting.config.Migrator;
import metadev.digital.MetaMobHunting.config.MigratorException;
import metadev.digital.MetaMobHunting.update.UpdateManager;
import metadev.digital.metacustomitemslib.server.Server;
import metadev.digital.metacustomitemslib.storage.DataStoreException;
import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.Tools;
import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.metacustomitemslib.compatibility.addons.CMICompat;
import metadev.digital.MetaMobHunting.Api.MobHuntingAPI;
import metadev.digital.MetaMobHunting.achievements.*;
import metadev.digital.MetaMobHunting.bounty.BountyManager;
import metadev.digital.MetaMobHunting.commands.BountyCommand;
import metadev.digital.MetaMobHunting.commands.CheckGrindingCommand;
import metadev.digital.MetaMobHunting.commands.ClearGrindingCommand;
import metadev.digital.MetaMobHunting.commands.CommandDispatcher;
import metadev.digital.MetaMobHunting.commands.DatabaseCommand;
import metadev.digital.MetaMobHunting.commands.DebugCommand;
import metadev.digital.MetaMobHunting.commands.HappyHourCommand;
import metadev.digital.MetaMobHunting.commands.HeadCommand;
import metadev.digital.MetaMobHunting.commands.HologramCommand;
import metadev.digital.MetaMobHunting.commands.LeaderboardCommand;
import metadev.digital.MetaMobHunting.commands.LearnCommand;
import metadev.digital.MetaMobHunting.commands.MoneyCommand;
import metadev.digital.MetaMobHunting.commands.AchievementsCommand;
import metadev.digital.MetaMobHunting.commands.BlacklistAreaCommand;
import metadev.digital.MetaMobHunting.commands.MuteCommand;
import metadev.digital.MetaMobHunting.commands.RegionCommand;
import metadev.digital.MetaMobHunting.commands.ReloadCommand;
import metadev.digital.MetaMobHunting.commands.SelectCommand;
import metadev.digital.MetaMobHunting.commands.TopCommand;
import metadev.digital.MetaMobHunting.commands.UpdateCommand;
import metadev.digital.MetaMobHunting.commands.VersionCommand;
import metadev.digital.MetaMobHunting.commands.WhitelistAreaCommand;
import metadev.digital.MetaMobHunting.compatibility.CompatibilityManager;
import metadev.digital.MetaMobHunting.compatibility.addons.BagOfGoldCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.BattleArenaCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.CitizensCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.CrackShotCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.EliteMobsCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.EssentialsCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.ExtraHardModeCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.FactionsUUIDCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.GringottsCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.LevelledMobsCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.LibsDisguisesCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.McMMOCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.MyPetCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.MobArenaCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.MythicMobsCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.PVPArenaCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.PlaceholderAPICompat;
import metadev.digital.MetaMobHunting.compatibility.addons.ResidenceCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.StackMobCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.TownyCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.VanishNoPacketCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.WeaponMechanicsCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.WorldEditCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.WorldGuardCompat;
import metadev.digital.MetaMobHunting.config.ConfigManager;
import metadev.digital.MetaMobHunting.grinding.GrindingManager;
import metadev.digital.MetaMobHunting.leaderboard.LeaderboardManager;
import metadev.digital.MetaMobHunting.mobs.ExtendedMobManager;
import metadev.digital.MetaMobHunting.rewards.RewardManager;
import metadev.digital.MetaMobHunting.storage.DataStoreManager;
import metadev.digital.MetaMobHunting.storage.IDataStore;
import metadev.digital.MetaMobHunting.storage.MySQLDataStore;
import metadev.digital.MetaMobHunting.storage.SQLiteDataStore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import io.chazza.advancementapi.AdvancementManager;

public class MobHunting extends JavaPlugin {

	public final static boolean disableAdvancements = true;

	private static MobHunting instance;
	public Random mRand = new Random();
	private File mFile = new File(getDataFolder(), "config.yml");

	private Messages mMessages;
	private ConfigManager mConfig;
	private EconomyManager mEconomyManager;
	private RewardManager mRewardManager;
	private MobHuntingManager mMobHuntingManager;
	private FishingManager mFishingManager;
	private GrindingManager mGrindingManager;
	private LeaderboardManager mLeaderboardManager;
	private AchievementManager mAchievementManager;
	private BountyManager mBountyManager;
	private ParticleManager mParticleManager = new ParticleManager();
	private MetricsManager mMetricsManager;
	private ExtendedMobManager mExtendedMobManager;
	private IDataStore mStore;
	private DataStoreManager mStoreManager;
	private AdvancementManager mAdvancementManager;
	private CommandDispatcher mCommandDispatcher;
	private CompatibilityManager mCompatibilityManager;
	private static UpdateManager mUpdateManager;

	private boolean mInitialized = false;
	public boolean disabling = false;

    // TODO: FINISH REMOVING DEPRECATED SERVER VERSION CALLS

    // PROJECT HEALTH REMAINING OBJECTIVES
    // TODO: ADD PLUGIN SHUTDOWN/SPINUP LOGIC MIRRORING MCIL
    // TODO: REWORK MOB ENTITY IF STATEMENT TREE HANDLING
    // TODO: ADD TRANSLATIONS FOR NEW COMPAT FEATURE CONSOLE MESSAGES & CONFIG MIGRATION PROCESS
    // TODO: AUDIT CONFIG AND APPLY A NEW VERSION
    // TODO: ADD UNIT TESTS VIA MOCKBUKKIT

    // PLUGIN COMPATIBILITIES
    // NEW
    // TODO: DiscordSRV
    // TODO: Finish integrating ImprovedFactions
    // OLD
    // TODO: AUDIT AND FULLY IMPLEMENT BOSSSHOP
    // TODO: AUDIT AND REIMPLEMENT ELITEMOBS
    // TODO: AUDIT TARDISWeepingAngels and add in support for the other mobs https://github.com/eccentricdevotion/TARDIS
    // TODO: AUDIT, PURCHASE, AND REIMPLEMENT BOSS https://mineacademy.org/boss  https://builtbybit.com/resources/boss-unbelievable-custom-monsters.21619/
    // TODO: TODO: Replace Reserve with https://bstats.org/plugin/bukkit/VaultUnlocked/22252 ex: https://github.com/TownyAdvanced/Towny/blob/d382a5d5b614ac5e2032b9e94bc861f2f313bf4c/Towny/src/main/java/com/palmergames/bukkit/towny/TownyEconomyHandler.java#L168



    @Override
	public void onLoad() {
		// Verify user is not running old Rocologo version and Meta version
		if (Bukkit.getPluginManager().getPlugin("MobHunting") != null) {
			throw new RuntimeException("[MetaMobHunting] Detected two versions of MobHunting running. Please remove the MobHunting jar if you wish to use MetaMobHunting.");
		}

		if (Bukkit.getPluginManager().getPlugin("CustomItemsLib") != null) {
			throw new RuntimeException("[MetaMobHunting] Detected a non-Meta or outdated version of MetaCustomItemsLib is running. Please validate your MetaCustomItemsLib " +
					"version is compatible if you wish to use MetaMobHunting.");
		}

		// Standup fresh config or migrate if old version exists
		if (!mFile.exists()) {
			File mFileOldConfigDir = new File(getDataFolder().getParent(), "MobHunting");
			try {
				Migrator.moveLegacyConfiguration(mFileOldConfigDir, getDataFolder());
			}
			catch (MigratorException e) {
				mFile.mkdir();
			}
		}

		instance = this;
		mMessages = new Messages(this);

		// Check what happen if WorldGuard is installed and register MobHuting
		// Flag
		Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");
		if (wg != null)
			WorldGuardCompat.registerFlag();

	}

	@Override
	public void onEnable() {

		disabling = false;

		int config_version = ConfigManager.getConfigVersion(mFile);
		Bukkit.getConsoleSender().sendMessage(
				ChatColor.GOLD + "[MetaMobHunting] " + ChatColor.RESET + "Your config version is " + config_version);
		switch (config_version) {
		case 0: // 0 was the old version number before MobHunting V5.0.0
		case -2:
			Bukkit.getConsoleSender().sendMessage(
					ChatColor.GOLD + "[MetaMobHunting] " + ChatColor.RESET + "Defect config.yml file. Deleted.");
		case -1:
			mConfig = new ConfigManager(this, mFile);
			if (!mConfig.loadConfig())
				Bukkit.getConsoleSender().sendMessage(
						ChatColor.GOLD + "[MetaMobHunting] " + ChatColor.RESET + "Error could not load config.yml");
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Creating new config.yml, version=" + mConfig.configVersion);
			break;
		default:
			mConfig = new ConfigManager(this, mFile);
			if (mConfig.loadConfig()) {
				Bukkit.getConsoleSender().sendMessage(
						ChatColor.GOLD + "[MetaMobHunting] " + ChatColor.RESET + "Existing config.yml loaded.");
				if (mConfig.backup)
					mConfig.backupConfig(mFile);
			} else
				throw new RuntimeException(getMessages().getString("mobhunting.config.fail"));
			break;
		}
		mConfig.saveConfig();

		if (isbStatsEnabled())
			MessageHelper.debug("bStat is enabled");
		else {
			MessageHelper.warning("=====================WARNING=============================");
			MessageHelper.warning("The statistics collection is disabled. As developer I need the");
			MessageHelper.warning("statistics from bStats.org. The statistics is 100% anonymous.");
			MessageHelper.warning("https://bstats.org/plugin/bukkit/MobHunting");
			MessageHelper.warning("Please enable this in /plugins/bStats/config.yml and get rid of this");
			MessageHelper.warning("message. Loading will continue in 15 sec.");
			MessageHelper.warning("=========================================================");
			long now = System.currentTimeMillis();
			while (System.currentTimeMillis() < now + 15000L) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
		}

		mCompatibilityManager = new CompatibilityManager(this);

		// Handle compatibility stuff
		mCompatibilityManager.registerPlugin(EssentialsCompat.class, SupportedPluginEntities.Essentials);
		mCompatibilityManager.registerPlugin(BagOfGoldCompat.class, SupportedPluginEntities.BagOfGold);
		mCompatibilityManager.registerPlugin(GringottsCompat.class, SupportedPluginEntities.Gringotts);

		// Hook into Vault or Reserve
		mEconomyManager = new EconomyManager(this);
		if (!mEconomyManager.isActive())
			return;

		mRewardManager = new RewardManager(this);

		mGrindingManager = new GrindingManager(this);

		if (mConfig.databaseType.equalsIgnoreCase("mysql"))
			mStore = new MySQLDataStore(this);
		else
			mStore = new SQLiteDataStore(this);

		try {
			mStore.initialize();
		} catch (DataStoreException e) {
			e.printStackTrace();
			try {
				mStore.shutdown();
			} catch (DataStoreException e1) {
				e1.printStackTrace();
			}
			setEnabled(false);
			return;
		}

		mStoreManager = new DataStoreManager(this, mStore);

		// Protection plugins
		mCompatibilityManager.registerPlugin(WorldEditCompat.class, SupportedPluginEntities.WorldEdit);
		mCompatibilityManager.registerPlugin(WorldGuardCompat.class, SupportedPluginEntities.WorldGuard);

		mCompatibilityManager.registerPlugin(CMICompat.class, SupportedPluginEntities.CMI);
		mCompatibilityManager.registerPlugin(ResidenceCompat.class, SupportedPluginEntities.Residence);
        mCompatibilityManager.registerPlugin(TownyCompat.class, SupportedPluginEntities.Towny);
        mCompatibilityManager.registerPlugin(FactionsUUIDCompat.class, SupportedPluginEntities.Factions);

		// Other plugins
		mCompatibilityManager.registerPlugin(McMMOCompat.class, SupportedPluginEntities.mcMMO);
		mCompatibilityManager.registerPlugin(MyPetCompat.class, SupportedPluginEntities.MyPet);

		// Minigame plugins
		mCompatibilityManager.registerPlugin(PVPArenaCompat.class, SupportedPluginEntities.PVPArena);
        mCompatibilityManager.registerPlugin(MobArenaCompat.class, SupportedPluginEntities.MobArena);
		mCompatibilityManager.registerPlugin(BattleArenaCompat.class, SupportedPluginEntities.BattleArena);

		// Disguise and Vanish plugins
		mCompatibilityManager.registerPlugin(LibsDisguisesCompat.class, SupportedPluginEntities.LibsDisguises);
        mCompatibilityManager.registerPlugin(VanishNoPacketCompat.class, SupportedPluginEntities.VanishNoPacket);

		// Plugin PlaceholderAPI
		mCompatibilityManager.registerPlugin(PlaceholderAPICompat.class, SupportedPluginEntities.PlaceholderAPI);

		// Plugins where the reward is a multiplier
		mCompatibilityManager.registerPlugin(StackMobCompat.class, SupportedPluginEntities.StackMob);
		mCompatibilityManager.registerPlugin(EliteMobsCompat.class, SupportedPluginEntities.EliteMobs);
		mCompatibilityManager.registerPlugin(LevelledMobsCompat.class, SupportedPluginEntities.LevelledMobs);

		// ExtendedMob Plugins where special mobs are created
		mCompatibilityManager.registerPlugin(MythicMobsCompat.class, SupportedPluginEntities.MythicMobs);
		mCompatibilityManager.registerPlugin(CitizensCompat.class, SupportedPluginEntities.Citizens);

		mCompatibilityManager.registerPlugin(ExtraHardModeCompat.class, SupportedPluginEntities.ExtraHardMode);
		mCompatibilityManager.registerPlugin(CrackShotCompat.class, SupportedPluginEntities.CrackShot);
		mCompatibilityManager.registerPlugin(WeaponMechanicsCompat.class, SupportedPluginEntities.WeaponMechanics);

		mExtendedMobManager = new ExtendedMobManager(this);

		// Register commands
		mCommandDispatcher = new CommandDispatcher(this, "mobhunt",
				getMessages().getString("mobhunting.command.base.description") + getDescription().getVersion());
		getCommand("mobhunt").setExecutor(mCommandDispatcher);
		getCommand("mobhunt").setTabCompleter(mCommandDispatcher);
		mCommandDispatcher.registerCommand(new AchievementsCommand(this));
		mCommandDispatcher.registerCommand(new BlacklistAreaCommand(this));
		mCommandDispatcher.registerCommand(new CheckGrindingCommand(this));
		mCommandDispatcher.registerCommand(new ClearGrindingCommand(this));
		mCommandDispatcher.registerCommand(new DatabaseCommand(this));
		mCommandDispatcher.registerCommand(new HeadCommand(this));
		mCommandDispatcher.registerCommand(new LeaderboardCommand(this));
		mCommandDispatcher.registerCommand(new HologramCommand(this));
		mCommandDispatcher.registerCommand(new LearnCommand(this));
		mCommandDispatcher.registerCommand(new MuteCommand(this));
		mCommandDispatcher.registerCommand(new ReloadCommand(this));
		if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.WorldGuard.getName())))
			mCommandDispatcher.registerCommand(new RegionCommand(this));
		if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.WorldEdit.getName())))
			mCommandDispatcher.registerCommand(new SelectCommand(this));
		mCommandDispatcher.registerCommand(new TopCommand(this));
		mCommandDispatcher.registerCommand(new WhitelistAreaCommand(this));
		mCommandDispatcher.registerCommand(new UpdateCommand(this));
		mCommandDispatcher.registerCommand(new VersionCommand(this));
		mCommandDispatcher.registerCommand(new DebugCommand(this));
		if (mConfig.enablePlayerBounties)
			mCommandDispatcher.registerCommand(new BountyCommand(this));
		mCommandDispatcher.registerCommand(new HappyHourCommand(this));
		mCommandDispatcher.registerCommand(new MoneyCommand(this));

		mLeaderboardManager = new LeaderboardManager(this);

		mAchievementManager = new AchievementManager(this);

		mMobHuntingManager = new MobHuntingManager(this);
		if (mConfig.enableFishingRewards)
			mFishingManager = new FishingManager(this);

		if (mConfig.enablePlayerBounties)
			mBountyManager = new BountyManager(this);

		if (!getConfigManager().disableMobHuntingAdvancements) {
			MessageHelper.debug("Updating advancements");
			mAdvancementManager = new AdvancementManager(this);
			if (!disableAdvancements)
				mAdvancementManager.getAdvancementsFromAchivements();
		}

		if (!Server.isGlowstoneServer()) {
			mMetricsManager = new MetricsManager(this);
			mMetricsManager.startBStatsMetrics();
		}

		// Handle online players when server admin do a /reload or /mh reload
		if (Tools.getOnlinePlayersAmount() > 0) {
			MessageHelper.debug("Reloading %s player settings from the database", Tools.getOnlinePlayersAmount());
			for (Player player : Tools.getOnlinePlayers()) {
				Core.getPlayerSettingsManager().load(player);
				mAchievementManager.load(player);
				if (mConfig.enablePlayerBounties)
					mBountyManager.load(player);
				mMobHuntingManager.setHuntEnabled(player, true);
			}
		}

		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run() {
				instance.getMessages().injectMissingMobNamesToLangFiles();
			}
		}, 20 * 5);

		// Check for new updates
		mUpdateManager = new UpdateManager(instance);
		mUpdateManager.processCheckResultInConsole();

		mInitialized = true;

	}

	@Override
	public void onDisable() {
		MessageHelper.debug("Disabling MobHunting.");
		disabling = true;

		if (!mInitialized)
			return;


		MessageHelper.debug("Shutdown LeaderBoardManager");
		mLeaderboardManager.shutdown();
		mGrindingManager.saveData();

		getMobHuntingManager().getHuntingModifiers().clear();
		if (mConfig.enableFishingRewards)
			getFishingManager().getFishingModifiers().clear();

        MessageHelper.debug("Shutting down compatibilities.");
        mCompatibilityManager.triggerSoftShutdown();

		try {
			MessageHelper.debug("Shutdown StoreManager");
			mStoreManager.shutdown();
			MessageHelper.debug("Shutdown Store");
			mStore.shutdown();
		} catch (DataStoreException e) {
			e.printStackTrace();
		}

		MessageHelper.debug("MobHunting disabled.");
	}

	private boolean isbStatsEnabled() {
		File bStatsFolder = new File(instance.getDataFolder().getParentFile(), "bStats");
		File configFile = new File(bStatsFolder, "config.yml");
		YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		return config.getBoolean("enabled", true);
	}

	// ************************************************************************************
	// Managers and handlers
	// ************************************************************************************
	public static MobHunting getInstance() {
		return instance;
	}

	public static MobHuntingAPI getAPI() {
		return MobHunting.getAPI();
	}

	public ConfigManager getConfigManager() {
		return mConfig;
	}

	/**
	 * Get the MessagesManager
	 * 
	 * @return
	 */
	public Messages getMessages() {
		return mMessages;
	}

	/**
	 * setMessages
	 * 
	 * @param messages
	 */
	public void setMessages(Messages messages) {
		mMessages = messages;
	}

	/**
	 * Gets the MobHuntingHandler
	 * 
	 * @return MobHuntingManager
	 */
	public MobHuntingManager getMobHuntingManager() {
		return mMobHuntingManager;
	}

	/**
	 * Get all Achievements for all players.
	 * 
	 * @return
	 */
	public AchievementManager getAchievementManager() {
		return mAchievementManager;
	}

	/**
	 * Gets the Store Manager
	 * 
	 * @return
	 */
	public IDataStore getStoreManager() {
		return mStore;
	}

	/**
	 * Gets the Database Store Manager
	 * 
	 * @return
	 */
	public DataStoreManager getDataStoreManager() {
		return mStoreManager;
	}

	/**
	 * Gets the LeaderboardManager
	 * 
	 * @return
	 */
	public LeaderboardManager getLeaderboardManager() {
		return mLeaderboardManager;
	}

	/**
	 * Get the BountyManager
	 * 
	 * @return
	 */
	public BountyManager getBountyManager() {
		return mBountyManager;
	}

	/**
	 * Get the AreaManager
	 * 
	 * @return
	 */
	public GrindingManager getGrindingManager() {
		return mGrindingManager;
	}

	/**
	 * Get the RewardManager
	 * 
	 * @return
	 */
	public RewardManager getRewardManager() {
		return mRewardManager;
	}

	/**
	 * Get the ParticleManager
	 * 
	 * @return
	 */
	public ParticleManager getParticleManager() {
		return mParticleManager;
	}

	/**
	 * Get the MobManager
	 * 
	 * @return
	 */
	public ExtendedMobManager getExtendedMobManager() {
		return mExtendedMobManager;
	}

	/**
	 * Get the FishingManager
	 * 
	 * @return
	 */
	public FishingManager getFishingManager() {
		return mFishingManager;
	}

	/**
	 * Get the AdvancementManager
	 * 
	 * @return
	 */
	public AdvancementManager getAdvancementManager() {
		return mAdvancementManager;
	}

	public CommandDispatcher getCommandDispatcher() {
		return mCommandDispatcher;
	}

	public CompatibilityManager getCompatibilityManager() {
		return mCompatibilityManager;
	}
	
	public static UpdateManager getUpdater() {	return mUpdateManager;	}

	public EconomyManager getEconomyManager() {
		return mEconomyManager;
	}

}

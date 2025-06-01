package metadev.digital.MetaMobHunting;

import java.io.File;
import java.util.Random;

import metadev.digital.MetaMobHunting.config.Migrator;
import metadev.digital.MetaMobHunting.config.MigratorException;
import metadev.digital.MetaMobHunting.update.UpdateManager;
import metadev.digital.metacustomitemslib.server.Servers;
import metadev.digital.metacustomitemslib.storage.DataStoreException;
import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.Tools;
import metadev.digital.metacustomitemslib.compatibility.CompatPlugin;
import metadev.digital.metacustomitemslib.compatibility.CMICompat;
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
import metadev.digital.MetaMobHunting.compatibility.BagOfGoldCompat;
import metadev.digital.MetaMobHunting.compatibility.BattleArenaCompat;
import metadev.digital.MetaMobHunting.compatibility.CitizensCompat;
import metadev.digital.MetaMobHunting.compatibility.CompatibilityManager;
import metadev.digital.MetaMobHunting.compatibility.CrackShotCompat;
import metadev.digital.MetaMobHunting.compatibility.EliteMobsCompat;
import metadev.digital.MetaMobHunting.compatibility.EssentialsCompat;
import metadev.digital.MetaMobHunting.compatibility.ExtraHardModeCompat;
import metadev.digital.MetaMobHunting.compatibility.GringottsCompat;
import metadev.digital.MetaMobHunting.compatibility.LevelledMobsCompat;
import metadev.digital.MetaMobHunting.compatibility.LibsDisguisesCompat;
import metadev.digital.MetaMobHunting.compatibility.McMMOCompat;
import metadev.digital.MetaMobHunting.compatibility.MyPetCompat;
import metadev.digital.MetaMobHunting.compatibility.MythicMobsCompat;
import metadev.digital.MetaMobHunting.compatibility.PVPArenaCompat;
import metadev.digital.MetaMobHunting.compatibility.PlaceholderAPICompat;
import metadev.digital.MetaMobHunting.compatibility.ResidenceCompat;
import metadev.digital.MetaMobHunting.compatibility.StackMobCompat;
import metadev.digital.MetaMobHunting.compatibility.WeaponMechanicsCompat;
import metadev.digital.MetaMobHunting.compatibility.WorldEditCompat;
import metadev.digital.MetaMobHunting.compatibility.WorldGuardCompat;
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

	// Constants
	private final static String pluginName = "mobhunting";

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

	//TODO Move Prefix and messages to Messages
	public static final String PREFIX = ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET;
	public static final String PREFIX_DEBUG = ChatColor.GOLD + "[MobHunting][Debug] " + ChatColor.RESET;
	public static final String PREFIX_WARNING = ChatColor.GOLD + "[MobHunting][Warning] " + ChatColor.RED;
	public static final String PREFIX_ERROR = ChatColor.GOLD + "[MobHunting][Error] " + ChatColor.RED;

	@Override
	public void onLoad() {
		// Verify user is not running old Rocologo version and Meta version
		if (Bukkit.getPluginManager().getPlugin("MobHunting") != null) {
			throw new RuntimeException("[MobHunting] Detected two versions of MobHunting running. Please remove the MobHunting jar if you wish to use MetaMobHunting.");
		}

		if (Bukkit.getPluginManager().getPlugin("CustomItemsLib") != null) {
			throw new RuntimeException("[MobHunting] Detected a non-Meta or outdated version of MetaCustomItemsLib is running. Please validate your MetaCustomItemsLib " +
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
				ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET + "Your config version is " + config_version);
		switch (config_version) {
		case 0: // 0 was the old version number before MobHunting V5.0.0
		case -2:
			Bukkit.getConsoleSender().sendMessage(
					ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET + "Defect config.yml file. Deleted.");
		case -1:
			mConfig = new ConfigManager(this, mFile);
			if (!mConfig.loadConfig())
				Bukkit.getConsoleSender().sendMessage(
						ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET + "Error could not load config.yml");
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Creating new config.yml, version=" + mConfig.configVersion);
			break;
		default:
			mConfig = new ConfigManager(this, mFile);
			if (mConfig.loadConfig()) {
				Bukkit.getConsoleSender().sendMessage(
						ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET + "Existing config.yml loaded.");
				if (mConfig.backup)
					mConfig.backupConfig(mFile);
			} else
				throw new RuntimeException(getMessages().getString(pluginName + ".config.fail"));
			break;
		}
		mConfig.saveConfig();

		if (isbStatsEnabled())
			getMessages().debug("bStat is enabled");
		else {
			Bukkit.getConsoleSender()
					.sendMessage(PREFIX_WARNING + "=====================WARNING=============================");
			Bukkit.getConsoleSender()
					.sendMessage(PREFIX_WARNING + "The statistics collection is disabled. As developer I need the");
			Bukkit.getConsoleSender()
					.sendMessage(PREFIX_WARNING + "statistics from bStats.org. The statistics is 100% anonymous.");
			Bukkit.getConsoleSender().sendMessage(PREFIX_WARNING + "https://bstats.org/plugin/bukkit/MobHunting");
			Bukkit.getConsoleSender().sendMessage(
					PREFIX_WARNING + "Please enable this in /plugins/bStats/config.yml and get rid of this");
			Bukkit.getConsoleSender().sendMessage(PREFIX_WARNING + "message. Loading will continue in 15 sec.");
			Bukkit.getConsoleSender()
					.sendMessage(PREFIX_WARNING + "=========================================================");
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
		mCompatibilityManager.registerPlugin(EssentialsCompat.class, CompatPlugin.Essentials);
		mCompatibilityManager.registerPlugin(BagOfGoldCompat.class, CompatPlugin.BagOfGold);
		mCompatibilityManager.registerPlugin(GringottsCompat.class, CompatPlugin.Gringotts);

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
		mCompatibilityManager.registerPlugin(WorldEditCompat.class, CompatPlugin.WorldEdit);
		mCompatibilityManager.registerPlugin(WorldGuardCompat.class, CompatPlugin.WorldGuard);

		mCompatibilityManager.registerPlugin(CMICompat.class, CompatPlugin.CMI);
		mCompatibilityManager.registerPlugin(CMICompat.class, CompatPlugin.CMILib);
		mCompatibilityManager.registerPlugin(ResidenceCompat.class, CompatPlugin.Residence);
		// Other plugins
		mCompatibilityManager.registerPlugin(McMMOCompat.class, CompatPlugin.mcMMO);
		mCompatibilityManager.registerPlugin(MyPetCompat.class, CompatPlugin.MyPet);

		// Minigame plugins
		mCompatibilityManager.registerPlugin(PVPArenaCompat.class, CompatPlugin.PVPArena);
		mCompatibilityManager.registerPlugin(BattleArenaCompat.class, CompatPlugin.BattleArena);

		// Disguise and Vanish plugins
		mCompatibilityManager.registerPlugin(LibsDisguisesCompat.class, CompatPlugin.LibsDisguises);

		// Plugin PlaceholderAPI
		mCompatibilityManager.registerPlugin(PlaceholderAPICompat.class, CompatPlugin.PlaceholderAPI);

		// Plugins where the reward is a multiplier
		mCompatibilityManager.registerPlugin(StackMobCompat.class, CompatPlugin.StackMob);
		mCompatibilityManager.registerPlugin(EliteMobsCompat.class, CompatPlugin.EliteMobs);
		mCompatibilityManager.registerPlugin(LevelledMobsCompat.class, CompatPlugin.LevelledMobs);

		// ExtendedMob Plugins where special mobs are created
		mCompatibilityManager.registerPlugin(MythicMobsCompat.class, CompatPlugin.MythicMobs);
		mCompatibilityManager.registerPlugin(CitizensCompat.class, CompatPlugin.Citizens);

		mCompatibilityManager.registerPlugin(ExtraHardModeCompat.class, CompatPlugin.ExtraHardMode);
		mCompatibilityManager.registerPlugin(CrackShotCompat.class, CompatPlugin.CrackShot);
		mCompatibilityManager.registerPlugin(WeaponMechanicsCompat.class, CompatPlugin.WeaponMechanics);

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
		if (WorldGuardCompat.isSupported())
			mCommandDispatcher.registerCommand(new RegionCommand(this));
		if (WorldEditCompat.isSupported())
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
			getMessages().debug("Updating advancements");
			mAdvancementManager = new AdvancementManager(this);
			if (!disableAdvancements)
				mAdvancementManager.getAdvancementsFromAchivements();
		}

		if (!Servers.isGlowstoneServer()) {
			mMetricsManager = new MetricsManager(this);
			mMetricsManager.startBStatsMetrics();
		}

		// Handle online players when server admin do a /reload or /mh reload
		if (Tools.getOnlinePlayersAmount() > 0) {
			getMessages().debug("Reloading %s player settings from the database", Tools.getOnlinePlayersAmount());
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
		getMessages().debug("Disabling MobHunting.");
		disabling = true;

		if (!mInitialized)
			return;

		getMessages().debug("Shutdown LeaderBoardManager");
		mLeaderboardManager.shutdown();
		mGrindingManager.saveData();
		if (PlaceholderAPICompat.isSupported()) {
			getMessages().debug("Shutdown PlaceHolderManager");
			PlaceholderAPICompat.shutdown();
		}
		getMobHuntingManager().getHuntingModifiers().clear();
		if (mConfig.enableFishingRewards)
			getFishingManager().getFishingModifiers().clear();

		try {
			getMessages().debug("Shutdown StoreManager");
			mStoreManager.shutdown();
			getMessages().debug("Shutdown Store");
			mStore.shutdown();
		} catch (DataStoreException e) {
			e.printStackTrace();
		}
		getMessages().debug("Shutdown CitizensCompat");
		CitizensCompat.shutdown();

		Bukkit.getConsoleSender().sendMessage(PREFIX + "MobHunting disabled.");
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

package metadev.digital.MetaMobHunting.compatibility;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
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
public class EliteMobsCompat implements Listener {

	// https://www.spigotmc.org/resources/%E2%9A%94elitemobs%E2%9A%94.40090/

	private static boolean supported = false;
	private static Plugin mPlugin;
	private static HashMap<String, ExtendedMobRewardData> mMobRewardData = new HashMap<String, ExtendedMobRewardData>();
	private static File file = new File(MobHunting.getInstance().getDataFolder(), "EliteMobs-rewards.yml");
	private static YamlConfiguration config = new YamlConfiguration();
	public static final String MH_ELITEMOBS = "MH:ELITEMOBS";
	private final String latestSupported = "6.5.0";

	public EliteMobsCompat() {
		if (!isEnabledInConfig()) {
			MessageHelper.warning("Compatibility with EliteMobs is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.EliteMobs.getName());

			if (mPlugin.getDescription().getVersion().compareTo(latestSupported) >= 0) {
				Bukkit.getPluginManager().registerEvents(this, MobHunting.getInstance());
				MessageHelper.notice("Enabling Compatibility with EliteMobs ("
						+ getEliteMobs().getDescription().getVersion() + ")");

				supported = true;

				loadEliteMobsMobsData();
				saveEliteMobsData();

			} else {
				MessageHelper.warning("Your current version of EliteMobs ("
						+ mPlugin.getDescription().getVersion()
						+ ") is not supported by MobHunting. Please upgrade to " + latestSupported + " or newer.");
			}
		}

	}

	// **************************************************************************
	// OTHER FUNCTIONS
	// **************************************************************************
	public EliteMobs getEliteMobs() {
		return (EliteMobs) mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEliteMobs(Entity entity) {
		if (isSupported())
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

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationEliteMobs;
	}

	public static HashMap<String, ExtendedMobRewardData> getMobRewardData() {
		return mMobRewardData;
	}

	public static int getProgressAchievementLevel1(String mobtype) {
		return mMobRewardData.get(mobtype).getAchivementLevel1();
	}

	// **************************************************************************
	// LOAD & SAVE
	// **************************************************************************
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

	// **************************************************************************
	// EVENTS
	// **************************************************************************

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
					new FixedMetadataValue(mPlugin, mMobRewardData.get(monster.name())));
		}
	}

}

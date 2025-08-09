package metadev.digital.MetaMobHunting.mobs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;

import metadev.digital.metacustomitemslib.mobs.MobType;
import metadev.digital.metacustomitemslib.storage.DataStoreException;
import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.compatibility.addons.CitizensCompat;
import metadev.digital.MetaMobHunting.compatibility.EliteMobsCompat;
import metadev.digital.MetaMobHunting.compatibility.MythicMobsCompat;

public class ExtendedMobManager {

	private MobHunting plugin;

	private HashMap<Integer, ExtendedMob> mobs = new HashMap<Integer, ExtendedMob>();

	public ExtendedMobManager(MobHunting plugin) {
		this.plugin = plugin;
		updateExtendedMobs();
	}

	public void updateExtendedMobs() {
		plugin.getStoreManager().insertMissingVanillaMobs();
		if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Citizens.getName())))
			plugin.getStoreManager().insertMissingCitizensMobs();
		if (MythicMobsCompat.isSupported())
			plugin.getStoreManager().insertMissingMythicMobs();
		if (EliteMobsCompat.isSupported())
			plugin.getStoreManager().insertEliteMobs();

		Set<ExtendedMob> set = new HashSet<ExtendedMob>();

		try {
			set = (HashSet<ExtendedMob>) plugin.getStoreManager().loadMobs();
		} catch (DataStoreException e) {
			MessageHelper.error("Could not load data from mh_Mobs");
			e.printStackTrace();
		}

		int n = 0;
		Iterator<ExtendedMob> mobset = set.iterator();
		while (mobset.hasNext()) {
			ExtendedMob mob = (ExtendedMob) mobset.next();
			switch (mob.getMobPlugin()) {
			case MythicMobs:
				if (!MythicMobsCompat.isSupported() || !MythicMobsCompat.isEnabledInConfig()
						|| !MythicMobsCompat.isMythicMob(mob.getMobtype()))
					continue;
				break;

			case Citizens:
				if (!MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Citizens.getName()))
						|| !CitizensCompat.isSentryOrSentinelOrSentries(mob.getMobtype()))
					continue;
				break;

			case EliteMobs:
				if (!EliteMobsCompat.isSupported() || !EliteMobsCompat.isEnabledInConfig())
					continue;
				break;

			case Minecraft:
				break;

			default:
				ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
				MessageHelper.error("Missing PluginType: " + mob.getMobPlugin().getName()
						+ " in ExtendedMobManager.");
				continue;
			}
			if (!mobs.containsKey(mob.getMob_id())) {
				n++;
				mobs.put(mob.getMob_id(), mob);
			}
		}
		MessageHelper.debug("%s mobs was loaded into memory. Total mobs=%s", n, mobs.size());

		MobHunting.getInstance().getMessages().injectMissingMobNamesToLangFiles();

	}

	public ExtendedMob getExtendedMobFromMobID(int i) {
		return mobs.get(i);
	}

	public HashMap<Integer, ExtendedMob> getAllMobs() {
		return mobs;
	}

	public int getMobIdFromMobTypeAndPluginID(String mobtype, MobPlugin mobPlugin) {
		Iterator<Entry<Integer, ExtendedMob>> mobset = mobs.entrySet().iterator();
		while (mobset.hasNext()) {
			ExtendedMob mob = (ExtendedMob) mobset.next().getValue();
			if (mob.getMobPlugin().equals(mobPlugin) && mob.getMobtype().equalsIgnoreCase(mobtype))
				return mob.getMob_id();
		}
		return 0;
	}

	// This is only used to get a "random" mob_id stored when an Achievement is
	// stored in mh_Daily
	public ExtendedMob getFirstMob() {
		int mob_id = mobs.keySet().iterator().next();
		return mobs.get(mob_id);
	}

	public String getTranslatedName() {
		return "";
	};

	public ExtendedMob getExtendedMobFromEntity(Entity entity) {
		int mob_id;
		MobPlugin mobPlugin;
		String mobtype;

		if (MythicMobsCompat.isMythicMob(entity)) {
			mobPlugin = MobPlugin.MythicMobs;
			mobtype = MythicMobsCompat.getMythicMobType(entity);
		} else if (CitizensCompat.isNPC(entity)) {
			mobPlugin = MobPlugin.Citizens;
			mobtype = String.valueOf(CitizensCompat.getNPCId(entity));
		} else if (EliteMobsCompat.isEliteMobs(entity)) {
			mobPlugin = MobPlugin.EliteMobs;
			mobtype = EliteMobsCompat.getEliteMobsType(entity).name();
		} else {
			// StatType
			mobPlugin = MobPlugin.Minecraft;
			MobType mob = MobType.getMobType(entity);
			if (mob != null)
				mobtype = mob.name();
			else
				mobtype = "";
		}
		mob_id = getMobIdFromMobTypeAndPluginID(mobtype, mobPlugin);
		return new ExtendedMob(mob_id, mobPlugin, mobtype);
	}

}

package one.lindegaard.MobHunting.compatibility;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.mobs.MobPlugin;
import one.lindegaard.MobHunting.mobs.ExtendedMobRewardData;

public class MythicMobsV500Compat implements Listener {

	private static Plugin mPlugin;

	public MythicMobsV500Compat() {
		mPlugin = Bukkit.getPluginManager().getPlugin("MythicMobs");
	}

	// **************************************************************************
	// OTHER FUNCTIONS
	// **************************************************************************
	private static MythicBukkit getMythicMobsV500() {
		return (MythicBukkit) mPlugin;
	}

	public static boolean isMythicMobV500(String killed) {
		if (MythicMobsCompat.isSupported())
			return getMythicMobV500(killed) != null;
		return false;
	}

	public static MythicMob getMythicMobV500(String killed) {
		if (MythicMobsCompat.isSupported())
			return getMythicMobsV500().getAPIHelper().getMythicMob(killed);
		return null;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationMythicmobs;
	}

	// **************************************************************************
	// EVENTS
	// **************************************************************************

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	private void onMythicMobV500SpawnEvent(MythicMobSpawnEvent event) {
		String mobtype = event.getMobType().getInternalName();

		if (!MythicMobsCompat.getMobRewardData().containsKey(mobtype)) {
			MobHunting.getInstance().getMessages().debug("New MythicMobType found=%s", mobtype);
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
				new FixedMetadataValue(mPlugin, MythicMobsCompat.getMobRewardData().get(mobtype)));
	}

	@SuppressWarnings("unused")
	private void onMythicMobV500DeathEvent(MythicMobDeathEvent event) {

	}

}

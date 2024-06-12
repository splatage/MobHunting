/**
 * TODO: Possibly deprecated

package metadev.digital.MetaMobHunting.compatibility;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobSpawnEvent;
import io.lumine.xikage.mythicmobs.mobs.MythicMob;
import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.mobs.MobPlugin;
import metadev.digital.MetaMobHunting.mobs.ExtendedMobRewardData;

public class MythicMobsV400Compat implements Listener {

	private static Plugin mPlugin;

	public MythicMobsV400Compat() {
		mPlugin = Bukkit.getPluginManager().getPlugin("MythicMobs");
	}

	// **************************************************************************
	// OTHER FUNCTIONS
	// **************************************************************************
	private static MythicMobs getMythicMobsV400() {
		return (MythicMobs) mPlugin;
	}

	public static boolean isMythicMobV400(String killed) {
		if (MythicMobsCompat.isSupported())
			return getMythicMobV400(killed) != null;
		return false;
	}

	public static MythicMob getMythicMobV400(String killed) {
		if (MythicMobsCompat.isSupported())
			return getMythicMobsV400().getAPIHelper().getMythicMob(killed);
		return null;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationMythicmobs;
	}

	// **************************************************************************
	// EVENTS
	// **************************************************************************

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	private void onMythicMobV400SpawnEvent(MythicMobSpawnEvent event) {
		String mobtype = event.getMobType().getInternalName();
		//MobHunting.getInstance().getMessages().debug("MythicMobSpawnEvent: MythicMobType=%s", mobtype);

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
	private void onMythicMobV400DeathEvent(MythicMobDeathEvent event) {

	}

}
 */
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

import net.elseland.xikage.MythicMobs.MythicMobs;
import net.elseland.xikage.MythicMobs.API.Bukkit.Events.MythicMobDeathEvent;
import net.elseland.xikage.MythicMobs.API.Bukkit.Events.MythicMobSpawnEvent;
import net.elseland.xikage.MythicMobs.API.Exceptions.InvalidMobTypeException;
import net.elseland.xikage.MythicMobs.Mobs.MythicMob;
import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.mobs.MobPlugin;
import metadev.digital.MetaMobHunting.mobs.ExtendedMobRewardData;

public class MythicMobsV251Compat implements Listener {

	private static Plugin mPlugin;

	public MythicMobsV251Compat() {
		mPlugin = Bukkit.getPluginManager().getPlugin("MythicMobs");
	}

	// **************************************************************************
	// OTHER FUNCTIONS
	// **************************************************************************
	private static MythicMobs getMythicMobsV251() {
		return (MythicMobs) mPlugin;
	}

	public static boolean isMythicMobV251(String killed) {
		if (MythicMobsCompat.isSupported())
			return getMythicMobV251(killed) != null;
		return false;
	}

	public static MythicMob getMythicMobV251(String killed) {
		if (MythicMobsCompat.isSupported())
			try {
				return getMythicMobsV251().getAPI().getMobAPI().getMythicMob(killed);
			} catch (InvalidMobTypeException e) {
				e.printStackTrace();
			}
		return null;
	}

	// **************************************************************************
	// EVENTS
	// **************************************************************************

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	private void onMythicMobV251SpawnEvent(MythicMobSpawnEvent event) {
		String mobtype = event.getMobType().getInternalName();
		MobHunting.getInstance().getMessages().debug("MythicMobSpawnEvent: MinecraftMobtype=%s MythicMobType=%s", event.getLivingEntity().getType(),
				mobtype);
		if (!MythicMobsCompat.getMobRewardData().containsKey(mobtype)) {
			MobHunting.getInstance().getMessages().debug("New MythicMobType found=%s (%s)", mobtype, event.getMobType().getDisplayName());
			MythicMobsCompat.getMobRewardData().put(mobtype,
					new ExtendedMobRewardData(MobPlugin.MythicMobs, mobtype, event.getMobType().getDisplayName(), 
							true,"10", 1, "You killed a Mythic mob",
							new ArrayList<HashMap<String,String>>(), 1, 0.02));
			MythicMobsCompat.saveMythicMobsData(mobtype);
			MobHunting.getInstance().getStoreManager().insertMissingMythicMobs(mobtype);
			// Update mob loaded into memory
			MobHunting.getInstance().getMessages().injectMissingMobNamesToLangFiles();
			MobHunting.getInstance().getExtendedMobManager().updateExtendedMobs();
		}

		event.getLivingEntity().setMetadata(MythicMobsCompat.MH_MYTHICMOBS, new FixedMetadataValue(mPlugin,
				MythicMobsCompat.getMobRewardData().get(event.getMobType().getInternalName())));
	}

	@SuppressWarnings("unused")
	private void onMythicMobV251DeathEvent(MythicMobDeathEvent event) {

	}

}
 */
package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.RegionQuery;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.managers.RegionManager;

public class WorldGuard6Helper {

public static boolean isAllowedByWorldGuard2(Entity damager, Entity damaged, StateFlag stateFlag,
			boolean defaultValue) {
		Player checkedPlayer = null;

		if (MyPetCompat.isMyPet(damager))
			checkedPlayer = MyPetCompat.getMyPetOwner(damager);
		else if (damager instanceof Player)
			checkedPlayer = (Player) damager;

		if (checkedPlayer != null) {
			Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");
			RegionManager regionManager = WorldGuardCompat.getWorldGuardPlugin().getRegionManager(checkedPlayer.getWorld());
			if (regionManager != null) {
				com.sk89q.worldguard.bukkit.RegionContainer container = ((WorldGuardPlugin)wg).getRegionContainer();
				RegionQuery query = container.createQuery();
				ApplicableRegionSet set = query.getApplicableRegions(checkedPlayer.getLocation());
				if (set.size() > 0) {
					LocalPlayer localPlayer = WorldGuardCompat.getWorldGuardPlugin().wrapPlayer(checkedPlayer);
					State flag = set.queryState(localPlayer, stateFlag);
					if (flag == null) {
						return defaultValue;
					} else if (flag.equals(State.ALLOW)) {
						return true;
					} else {
						return false;
					}
				} else {
					return defaultValue;
				}
			}
		}
		return defaultValue;
	}

	public static void registerFlag2() {
		Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");
		try {
			// register MobHuting flag with the WorlsGuard Flag registry

			// wg7.x
			//WorldGuard.getInstance().getFlagRegistry().register(WorldGuardMobHuntingFlag.getMobHuntingFlag());

			// wg6.x
			((WorldGuardPlugin) wg).getFlagRegistry().register(WorldGuardMobHuntingFlag.getMobHuntingFlag());
		} catch (FlagConflictException e) {

			// some other plugin registered a flag by the same name already.
			// you may want to re-register with a different name, but this
			// could cause issues with saved flags in region files. it's
			// better
			// to print a message to let the server admin know of the
			// conflict
		}
	}

}

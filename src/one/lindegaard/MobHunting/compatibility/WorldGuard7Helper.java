package one.lindegaard.MobHunting.compatibility;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

public class WorldGuard7Helper {

public static boolean isAllowedByWorldGuard2(Entity damager, Entity damaged, StateFlag stateFlag,
			boolean defaultValue) {
		Player checkedPlayer = null;

		if (MyPetCompat.isMyPet(damager))
			checkedPlayer = MyPetCompat.getMyPetOwner(damager);
		else if (damager instanceof Player)
			checkedPlayer = (Player) damager;

		if (checkedPlayer != null) {
			LocalPlayer localPlayer = WorldGuardCompat.getWorldGuardPlugin().wrapPlayer(checkedPlayer);
			Location loc = localPlayer.getLocation();
			RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
			if (container != null) {
				// https://worldguard.enginehub.org/en/latest/developer/regions/spatial-queries/
				RegionQuery query = container.createQuery();
				ApplicableRegionSet set = query.getApplicableRegions(loc);
				if (set.size() > 0) {
					State flag = set.queryState(localPlayer, stateFlag);
					if (flag != null) {
						return flag == State.ALLOW;
					}
				}
				return defaultValue;
			}
		}
		return defaultValue;
	}

	public static void registerFlag2() {
		//Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");
		try {
			// register MobHuting flag with the WorlsGuard Flag registry

			// wg7.x
			WorldGuard.getInstance().getFlagRegistry().register(WorldGuardMobHuntingFlag.getMobHuntingFlag());

			// wg6.x
			// ((WorldGuardPlugin)
			// wg).getFlagRegistry().register(WorldGuardHelper.getMobHuntingFlag());
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

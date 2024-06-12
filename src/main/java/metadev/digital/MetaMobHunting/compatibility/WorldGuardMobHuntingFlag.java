package metadev.digital.MetaMobHunting.compatibility;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class WorldGuardMobHuntingFlag {

	private static final StateFlag MOBHUNTING_FLAG = new StateFlag("mobhunting", false);
	private static final StateFlag MOB_DAMAGE_FLAG = new StateFlag("mob-damage", true);

	// *******************************************************************
	// getters
	// *******************************************************************

	public static StateFlag getMobHuntingFlag() {
		return MOBHUNTING_FLAG;
	}

	public static StateFlag getMobDamageFlag() {
		if (WorldGuardCompat.getWorldGuardPlugin().getDescription().getVersion().compareTo("7.0.0") >= 0) {
			return Flags.MOB_DAMAGE;
		} else {
			return MOB_DAMAGE_FLAG;
		}
	}

	// *******************************************************************
	// SET / REMOVE FLAG
	// *******************************************************************

	private static State parseInput(String flagValue) {
		if (flagValue.equalsIgnoreCase("allow"))
			return State.ALLOW;
		else if (flagValue.equalsIgnoreCase("deny"))
			return State.DENY;
		else
			return null;
	}

	public static boolean setCurrentRegionFlag(CommandSender sender, World world, ProtectedRegion region,
			StateFlag stateFlag, String flagstate) {
		if (region.getFlag(getMobHuntingFlag()) != null) {
			Map<Flag<?>, Object> flags = region.getFlags();
			flags.remove(MOBHUNTING_FLAG);
			region.setFlags(flags);
		}
		region.setFlag(getMobHuntingFlag(), parseInput(flagstate));
		if (sender != null)
			sender.sendMessage(
					ChatColor.YELLOW + "Region flag MobHunting set on '" + region.getId() + "' to '" + flagstate + "'");
		String flagstring = "";
		Iterator<Entry<Flag<?>, Object>> i = region.getFlags().entrySet().iterator();
		while (i.hasNext()) {
			Entry<Flag<?>, Object> s = i.next();
			flagstring = flagstring + s.getKey().getName() + ": " + s.getValue();
			if (i.hasNext())
				flagstring = flagstring + ",";
		}
		if (sender != null)
			sender.sendMessage(ChatColor.GRAY + "(Current flags: " + flagstring + ")");

		return true;
	}

	public static boolean removeCurrentRegionFlag(CommandSender sender, World world, ProtectedRegion region,
			StateFlag stateFlag) {
		region.setFlag(stateFlag, null);
		if (sender != null)
			sender.sendMessage(ChatColor.YELLOW + "Region flag '" + stateFlag.getName() + "' removed from region '"
					+ region.getId() + "'");
		return true;
	}

	
}

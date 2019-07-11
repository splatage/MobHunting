package one.lindegaard.MobHunting.npc;

import org.bukkit.Material;
import org.bukkit.block.Block;

import one.lindegaard.Core.Server.Servers;

public class MasterMobHunterRedstoneLamp {

	public MasterMobHunterRedstoneLamp() {
	}

	public static void setPowerOnRedstoneLamp(Block lamp, byte power) {
		if (Servers.isMC113OrNewer()) {
			MasterMobHunterRedstoneLamp1_13.setPowerOnRedstoneLamp(lamp, power);
		} else {
			lamp.setType(Material.matchMaterial("REDSTONE_LAMP_ON"));
			lamp.getState().update();
		}
	}

	public static void removePowerFromredstoneLamp(Block lamp) {
		if (Servers.isMC113OrNewer()) {
			MasterMobHunterRedstoneLamp1_13.removePowerFromredstoneLamp(lamp);
		} else {
			lamp.setType(Material.matchMaterial("REDSTONE_LAMP_OFF"));
			lamp.getState().update();
		}
	}

	public static boolean isRedstoneLamp(Block block) {
		return block.getType().equals(Material.matchMaterial("REDSTONE_LAMP"))
				|| block.getType().equals(Material.matchMaterial("LIT_REDSTONE_LAMP"))
				|| block.getType().equals(Material.matchMaterial("REDSTONE_LAMP_OFF"))
				|| block.getType().equals(Material.matchMaterial("REDSTONE_LAMP_ON"));
	}

}

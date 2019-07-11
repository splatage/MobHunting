package one.lindegaard.MobHunting.npc;

import org.bukkit.Material;
import org.bukkit.block.Block;

import one.lindegaard.Core.Server.Servers;

public class MasterMobHunterRedstoneWire {

	public MasterMobHunterRedstoneWire() {
	}

	public static void setPowerOnRedstoneWire(Block block, byte power) {
		if (Servers.isMC113OrNewer()) {
			MasterMobHunterRedstoneWire1_13.setPowerOnRedstoneWire(block, power);
		} else {
			block.setType(Material.matchMaterial("REDSTONE_WIRE"));
			block.getState().setRawData(power);
			block.getState().update();
		}
	}

	public static void removePowerFromRedstoneWire(Block block) {
		if (Servers.isMC113OrNewer()) {
			MasterMobHunterRedstoneWire1_13.removePowerFromRedstoneWire(block);
		} else {
			block.getState().setRawData((byte) 0);
			block.getState().update();
		}
	}

	public static boolean isRedstoneWire(Block block) {
		return block.getType().equals(Material.REDSTONE_WIRE);
	}
	
}

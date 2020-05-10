package one.lindegaard.MobHunting.npc;

import org.bukkit.Material;
import org.bukkit.block.Block;

import one.lindegaard.Core.server.Servers;
import one.lindegaard.MobHunting.MobHunting;

public class MasterMobHunterRedstoneWire {

	public MasterMobHunterRedstoneWire() {
	}

	public static void setPowerOnRedstoneWire(Block block, byte power) {
		if (Servers.isMC113OrNewer()) {
			MasterMobHunterRedstoneWire1_13.setPowerOnRedstoneWire(block, power);
		} else {

			// this current doesn't work on servers older than mc 1.13.
			MobHunting.getAPI().getMessages().debug("Set power on RedStone");
			//block.setType(Material.matchMaterial("REDSTONE_WIRE"));
			//block.setTypeIdAndData(Material.REDSTONE_WIRE.getId(), power, true);
			//block.getState().update();
			block.setType(Material.REDSTONE_WIRE);
			block.getState().setRawData(power);
			block.getState().update(true, false);
		}
	}

	public static void removePowerFromRedstoneWire(Block block) {
		if (Servers.isMC113OrNewer()) {
			MasterMobHunterRedstoneWire1_13.removePowerFromRedstoneWire(block);
		} else {
			
			// this current doesn't work on servers older than mc 1.13.
			MobHunting.getAPI().getMessages().debug("Remove power on RedStone");
			block.setType(Material.REDSTONE_WIRE);
			block.getState().setRawData((byte) 0);
			block.getState().update(true, false);
		}
	}

	public static boolean isRedstoneWire(Block block) {
		return block.getType().equals(Material.REDSTONE_WIRE);
	}
	
}

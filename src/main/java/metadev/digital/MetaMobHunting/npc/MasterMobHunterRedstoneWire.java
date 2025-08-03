package metadev.digital.MetaMobHunting.npc;

import org.bukkit.Material;
import org.bukkit.block.Block;

import metadev.digital.metacustomitemslib.server.Server;
import metadev.digital.MetaMobHunting.MobHunting;

public class MasterMobHunterRedstoneWire {

	public MasterMobHunterRedstoneWire() {
	}

	public static void setPowerOnRedstoneWire(Block block, byte power) {
		if (Server.isMC113OrNewer()) {
			MasterMobHunterRedstoneWire1_13.setPowerOnRedstoneWire(block, power);
		} else {

			// this current doesn't work on servers older than mc 1.13.
			MobHunting.getInstance().getMessages().debug("Set power on RedStone");
			//block.setType(Material.matchMaterial("REDSTONE_WIRE"));
			//block.setTypeIdAndData(Material.REDSTONE_WIRE.getId(), power, true);
			//block.getState().update();
			block.setType(Material.REDSTONE_WIRE);
			block.getState().setRawData(power);
			block.getState().update(true, false);
		}
	}

	public static void removePowerFromRedstoneWire(Block block) {
		if (Server.isMC113OrNewer()) {
			MasterMobHunterRedstoneWire1_13.removePowerFromRedstoneWire(block);
		} else {
			
			// this current doesn't work on Server older than mc 1.13.
			MobHunting.getInstance().getMessages().debug("Remove power on RedStone");
			block.setType(Material.REDSTONE_WIRE);
			block.getState().setRawData((byte) 0);
			block.getState().update(true, false);
		}
	}

	public static boolean isRedstoneWire(Block block) {
		return block.getType().equals(Material.REDSTONE_WIRE);
	}
	
}

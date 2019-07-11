package one.lindegaard.MobHunting.npc;

import org.bukkit.block.Block;

public class MasterMobHunterRedstoneWire1_13 {

	public MasterMobHunterRedstoneWire1_13() {
	}

	public static void setPowerOnRedstoneWire(Block block, byte power) {
			org.bukkit.block.data.type.RedstoneWire redstonewire = (org.bukkit.block.data.type.RedstoneWire) block.getBlockData();
			redstonewire.setPower(power);
			block.setBlockData(redstonewire);
	}

	public static void removePowerFromRedstoneWire(Block block) {
			block.getState().setRawData((byte) 0);
			block.getState().update();
	}
	
}

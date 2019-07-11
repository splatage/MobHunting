package one.lindegaard.MobHunting.npc;

import org.bukkit.block.Block;

public class MasterMobHunterPiston1_13 {

	public MasterMobHunterPiston1_13() {
	}

	public static void setPowerOnPiston(Block block) {
		org.bukkit.block.data.type.Piston piston = (org.bukkit.block.data.type.Piston) block.getBlockData();
		piston.setExtended(true);
		block.setBlockData(piston);
	}

	public static void removePowerOnPiston(Block block) {
		org.bukkit.block.data.type.Piston piston = (org.bukkit.block.data.type.Piston) block.getBlockData();
		piston.setExtended(false);
		block.setBlockData(piston);
	}

}

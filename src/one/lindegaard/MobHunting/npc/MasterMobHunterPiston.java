package one.lindegaard.MobHunting.npc;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.PistonBaseMaterial;
import org.bukkit.material.PistonExtensionMaterial;

import one.lindegaard.Core.Server.Servers;

public class MasterMobHunterPiston {

	public MasterMobHunterPiston() {
	}

	public static void setPowerOnPiston(Block block) {
		if (Servers.isMC113OrNewer()) {
			MasterMobHunterPiston1_13.setPowerOnPiston(block);
		} else {
			PistonBaseMaterial pistonData = (PistonBaseMaterial) block.getState().getData();
			if (!pistonData.isPowered()) {
				pistonData.setPowered(true);

				block.getState().setRawData(pistonData.getData());
				block.getState().update();

				BlockFace blockFace = pistonData.getFacing();
				Block tb = block.getRelative(blockFace);
				tb.setType(Material.matchMaterial("PISTON_EXTENSION"), false);
				PistonExtensionMaterial pistonExtentionData = (PistonExtensionMaterial) tb.getState().getData();
				pistonExtentionData.setFacingDirection(block.getFace(tb));
				tb.getState().setRawData(pistonExtentionData.getData());
				tb.getState().update(true, false);
			}
		}
	}

	public static void removePowerOnPiston(Block block) {
		if (Servers.isMC113OrNewer()) {
			MasterMobHunterPiston1_13.removePowerOnPiston(block);
		} else {
			PistonBaseMaterial pistonData = (PistonBaseMaterial) block.getState().getData();
			if (!pistonData.isPowered()) {
				pistonData.setPowered(false);
				block.getState().setRawData(pistonData.getData());
				block.getState().update(true, false);

				BlockFace blockFace = pistonData.getFacing();
				Block tb = block.getRelative(blockFace);
				tb.setType(Material.matchMaterial("PISTON_EXTENSION"), false);
				PistonExtensionMaterial pistonExtentionData = (PistonExtensionMaterial) tb.getState().getData();
				pistonExtentionData.setFacingDirection(block.getFace(tb));
				tb.getState().setRawData(pistonExtentionData.getData());
				tb.getState().update(true, false);
			}
		}
	}

	public static boolean isPiston(Block block) {
		return block.getType().equals(Material.matchMaterial("PISTON"))
				|| block.getType().equals(Material.matchMaterial("PISTON_STICKY"))
				|| block.getType().equals(Material.matchMaterial("PISTON_BASE"))
				|| block.getType().equals(Material.matchMaterial("PISTON_EXTENSION"))
				|| block.getType().equals(Material.matchMaterial("PISTON_HEAD"))
				|| block.getType().equals(Material.matchMaterial("PISTON_MOVING_PIECE"))
				|| block.getType().equals(Material.matchMaterial("PISTON_STICKY_BASE"));
	}

	public static boolean isPistonBase(Block block) {
		return block.getType().equals(Material.matchMaterial("PISTON_BASE"))
				|| block.getType().equals(Material.matchMaterial("PISTON_STICKY_BASE"));
	}

}

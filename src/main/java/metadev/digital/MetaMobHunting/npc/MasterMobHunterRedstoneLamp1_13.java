package metadev.digital.MetaMobHunting.npc;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class MasterMobHunterRedstoneLamp1_13 {

	public MasterMobHunterRedstoneLamp1_13() {
	}

	public static void setPowerOnRedstoneLamp(Block lamp, byte power) {
			if (MasterMobHunterSign.isMHIndirectPoweredBySign(lamp)) {
				for (BlockFace bf : MasterMobHunterSign.possibleBlockface) {
					Block rb = lamp.getRelative(bf);
					if (MasterMobHunterSign.isMHPoweredSign(rb)) {
						org.bukkit.block.data.Lightable lightable = (org.bukkit.block.data.Lightable) lamp.getBlockData();
						lightable.setLit(true);
						lamp.setBlockData(lightable);
					}
				}
			}
			// MC 1.12
			// boolean lit = lamp.getType() ==
			// Material.matchMaterial(XMaterial.REDSTONE_LAMP.getLegacy()[1]); // Like
			// computers 0 off, 1 on
	}

	public static void removePowerFromredstoneLamp(Block lamp) {
			org.bukkit.block.data.Lightable lightable = (org.bukkit.block.data.Lightable) lamp.getBlockData();
			lightable.setLit(false);
			lamp.setBlockData(lightable);
	}

}

package one.lindegaard.MobHunting.util;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;

import one.lindegaard.Core.Core;
import one.lindegaard.Core.server.Servers;
import one.lindegaard.MobHunting.leaderboard.WorldLeaderBoardHelper;

public class Misc {

	public static String trimSignText(String string) {
		return string.length() > 15 ? string.substring(0, 14).trim() : string;
	}

	public static double round(double d) {
		return Math.round(d / Core.getConfigManager().rewardRounding)
				* Core.getConfigManager().rewardRounding;
	}

	public static double ceil(double d) {
		return Math.ceil(d / Core.getConfigManager().rewardRounding)
				* Core.getConfigManager().rewardRounding;
	}

	public static double floor(double d) {
		return Math.floor(d / Core.getConfigManager().rewardRounding)
				* Core.getConfigManager().rewardRounding;
	}
	
	public static void setSignBlock(Block signBlock, BlockFace mFacing) {

		if (signBlock.getChunk().isLoaded())
			if (Servers.isMC114OrNewer()) {
				WorldLeaderBoardHelper.setWallSign1_14(signBlock, mFacing);
			} else if (Servers.isMC113OrNewer()) {
				signBlock.setType(Material.matchMaterial("WALL_SIGN"));
				BlockState state = signBlock.getState();
				org.bukkit.material.Sign wallSign = (org.bukkit.material.Sign) state.getData();
				wallSign.setFacingDirection(mFacing);
				state.setData(wallSign);
				state.update(true, false);
			} else {
				org.bukkit.material.Sign sign = new org.bukkit.material.Sign(Material.matchMaterial("WALL_SIGN"));
				sign.setFacingDirection(mFacing);
				BlockState state = signBlock.getState();
				state.setType(Material.matchMaterial("WALL_SIGN"));
				state.setData(sign);
				state.update(true, false);

			}
	}

}

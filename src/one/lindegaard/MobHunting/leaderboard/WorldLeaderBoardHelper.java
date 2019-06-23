package one.lindegaard.MobHunting.leaderboard;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.WallSign;

public class WorldLeaderBoardHelper {

	public static void setWallSign1_14(Block signBlock, BlockFace facing) {
		signBlock.setType(Material.OAK_WALL_SIGN);
		BlockState state = signBlock.getState();
		WallSign wallSign = (WallSign) state.getBlockData();
		wallSign.setFacing(facing);
		state.setBlockData(wallSign);
		state.update(true, false);
	}
}

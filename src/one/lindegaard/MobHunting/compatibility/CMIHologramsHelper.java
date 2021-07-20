package one.lindegaard.MobHunting.compatibility;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import com.Zrips.CMI.Modules.Holograms.CMIHologram;

import net.Zrips.CMILib.Container.CMILocation;
import one.lindegaard.MobHunting.leaderboard.HologramLeaderboard;

public class CMIHologramsHelper {

	public static void createHologram(HologramLeaderboard board) {
		Location loc = board.getLocation().subtract(0, 1, 0);
		CMIHologram hologram = new CMIHologram(board.getHologramName(), new CMILocation(loc));
		CMICompat.getHologramManager().addHologram(hologram);
		hologram.enable();
		hologram.update();
	}

	public static void addTextLine(CMIHologram hologram, String text) {
		List<String> lines = hologram.getLines();
		lines.add(lines.size(), text);
		hologram.setLines(lines);
		hologram.update();
	}

	public static void removeLine(CMIHologram hologram, int i) {
		hologram.removeLine(i);
		hologram.update();
	}

	public static void editTextLine(CMIHologram hologram, String text, int i) {
		if (i < hologram.getHeight())
			hologram.addLine(text);
		else
			hologram.setLine(i, text);
		hologram.update();
	}

	public static void addItemLineXXX(CMIHologram hologram, ItemStack itemstack) {
		// hologram.addLine(new ItemLine(hologram, itemstack));
	}

	public static void deleteHologram(CMIHologram hologram) {
		hologram.disable();
		CMICompat.getHologramManager().removeHolo(hologram);
	}

	public static void hideHologram(CMIHologram hologram) {
		CMICompat.getHologramManager().hideHoloForAllPlayers(hologram);
	}

}

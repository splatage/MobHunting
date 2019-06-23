package one.lindegaard.MobHunting.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
//import org.bukkit.block.Sign;
import org.bukkit.material.PistonBaseMaterial;
import org.bukkit.material.PistonExtensionMaterial;

import one.lindegaard.Core.Materials.Materials;
import one.lindegaard.Core.Server.Servers;
import one.lindegaard.MobHunting.MobHunting;

public class MasterMobHunterTools {

	public final static String MH_SIGN = "MH:sign";
	public final static byte POWER_FROM_SIGN = 15;
	public final static String MH_POWERED = "MH:powered";

	// https://regex101.com/
	// Regex string="\[(MH|mh|Mh|mH)(\d+)(\+)?\]"
	// Example: [mh001+]
	final static String MASTERMOBHUNTERSIGN = "\\[(MH|mh|Mh|mH)(\\d+)(\\+)?\\]";

	public static List<Material> supportedmats = new ArrayList<Material>();

	public MasterMobHunterTools() {
		supportedmats.add(Material.matchMaterial("REDSTONE_LAMP_OFF"));
		supportedmats.add(Material.matchMaterial("REDSTONE_LAMP_ON"));
		supportedmats.add(Material.matchMaterial("REDSTONE_TORCH_OFF"));
		supportedmats.add(Material.matchMaterial("REDSTONE_TORCH_ON"));
		supportedmats.add(Material.REDSTONE_WIRE);
	}

	public static boolean isMHSign(Block block) {
		if (Materials.isSign(block)) {
			org.bukkit.block.Sign sign;
			if (Servers.isMC113OrNewer()) {
				sign = (org.bukkit.block.Sign) block.getState();
			} else {
				// BlockState state = block.getState();
				if (block.getState() instanceof org.bukkit.block.Sign) {
					sign = (org.bukkit.block.Sign) block.getState();
				} else
					return false;
			}

			if (sign.getLine(0).matches(MASTERMOBHUNTERSIGN))
				return true;
			else if (block.hasMetadata(MH_SIGN))
				return true;
			else if (sign.hasMetadata(MH_SIGN))
				return true;
		}
		return false;
	}

	public static boolean isMHSign(String line) {
		return line.matches(MASTERMOBHUNTERSIGN);
	}

	public static int getNPCIdOnSign(Block block) {
		if (!Materials.isSign(block))
			return -1;

		String str = ((org.bukkit.block.Sign) block.getState()).getLine(0);
		if (str.matches(MASTERMOBHUNTERSIGN)) {
			// block.setMetadata(MH_SIGN, new
			// FixedMetadataValue(MobHunting.getInstance(), str));
			// MobHunting.getInstance().getMessages().debug("(186)MH Sign updated=%s",
			// str);

			// TODO: cleanup
		} else if (block.hasMetadata(MH_SIGN)) {
			String md = block.getMetadata(MH_SIGN).get(0).asString();
			if (md.matches(MASTERMOBHUNTERSIGN))
				str = md;
		}
		if (str.matches(MASTERMOBHUNTERSIGN)) {
			Pattern pattern = Pattern.compile(MASTERMOBHUNTERSIGN);
			Matcher m = pattern.matcher(str);
			m.find();
			return Integer.valueOf(m.group(2));
		} else
			return -1;
	}

	public static int getNPCIdOnSign(String str) {
		if (str.matches(MASTERMOBHUNTERSIGN)) {
			Pattern pattern = Pattern.compile(MASTERMOBHUNTERSIGN);
			Matcher m = pattern.matcher(str);
			m.find();
			return Integer.valueOf(m.group(2));
		} else
			return -1;
	}

	public static void removePowerOnPiston(Block b) {
		PistonBaseMaterial pistonData = (PistonBaseMaterial) b.getState().getData();
		if (!pistonData.isPowered()) {
			pistonData.setPowered(false);
			b.getState().setRawData(pistonData.getData());
			b.getState().update(true, false);

			Bukkit.getConsoleSender().sendMessage("MASTERMOBHUNTERSIGN: 4446");
			BlockFace blockFace = pistonData.getFacing();
			Bukkit.getConsoleSender().sendMessage("MASTERMOBHUNTERSIGN: 4447");
			Block tb = b.getRelative(blockFace);
			tb.setType(Material.matchMaterial("PISTON_EXTENSION"), false);
			PistonExtensionMaterial pistonExtentionData = (PistonExtensionMaterial) tb.getState().getData();
			pistonExtentionData.setFacingDirection(b.getFace(tb));
			tb.getState().setRawData(pistonExtentionData.getData());
			tb.getState().update(true, false);
		}
	}

	public static void removePower(Block block) {
		if (isMHPowered(block)) {
			block.removeMetadata(MH_POWERED, MobHunting.getInstance());
			for (BlockFace bf : possibleBlockface) {
				Block rb = block.getRelative(bf);
				// MobHunting.getInstance().getMessages().debug("rb = %s, isPowered=%s,
				// !isMHPoweredSign=%s",
				// rb.getType(), isMHPowered(rb),
				// !isMHPoweredSign(rb));
				if (rb != null && isMHPowered(rb) && !isMHPoweredSign(rb) && supportedmats.contains(rb.getType())) {
					// MobHunting.getInstance().getMessages().debug("remove power on %s",
					// rb.getType());
					if (rb.getType().equals(Material.matchMaterial("REDSTONE_LAMP_ON"))) {
						rb.setType(Material.matchMaterial("REDSTONE_LAMP_OFF"));
						// MobHunting.getInstance().getMessages().debug("Turn Redstone Lamp OFF");
						// BlockRedstoneEvent bre = new BlockRedstoneEvent(rb,
						// 15, 0);
						// Bukkit.getServer().getPluginManager().callEvent(bre);
					} else if (rb.getType().equals(Material.REDSTONE_WIRE)) {
						rb.getState().setRawData((byte) 0);
						rb.getState().update();
						// BlockRedstoneEvent bre = new BlockRedstoneEvent(rb,
						// 15, 0);
						// Bukkit.getServer().getPluginManager().callEvent(bre);
					} else if (rb.getType().equals(Material.matchMaterial("PISTON_BASE"))
							|| (rb.getType().equals(Material.matchMaterial("PISTON_STICKY_BASE")))) {
						MasterMobHunterTools.removePowerOnPiston(rb);
						// BlockRedstoneEvent bre = new BlockRedstoneEvent(rb,
						// 15, 0);
						// Bukkit.getServer().getPluginManager().callEvent(bre);
					}
					removeMHPower(rb);
					// BlockRedstoneEvent bre = new BlockRedstoneEvent(rb, 15,
					// 0);
					// Bukkit.getServer().getPluginManager().callEvent(bre);
				}
			}
		}

	}

	public static boolean isPowerSetOnSign(Block block) {
		String str;
		if (MasterMobHunterTools.isMHSign(block)) {
			if (block.hasMetadata(MH_SIGN))
				str = block.getMetadata(MH_SIGN).get(0).asString();
			else
				str = ((org.bukkit.block.Sign) block.getState()).getLine(0);
			if (str.matches(MASTERMOBHUNTERSIGN)) {
				Pattern pattern = Pattern.compile(MASTERMOBHUNTERSIGN);
				Matcher m = pattern.matcher(str);
				m.find();
				return (m.group(3) == null) ? false : true;
			}
		}
		return false;
	}

	public final static BlockFace possibleBlockface[] = new BlockFace[] { BlockFace.NORTH, BlockFace.EAST,
			BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN };

	public static void removeMHPower(Block block) {
		block.removeMetadata(MH_POWERED, MobHunting.getInstance());
		for (BlockFace bf : possibleBlockface) {
			Block rb = block.getRelative(bf);
			if (isMHPowered(rb) && !isMHPoweredSign(rb))
				removeMHPower(rb);
		}
	}

	public static boolean isMHPoweredSign(Block block) {
		if (MasterMobHunterTools.isMHSign(block) && isMHPowered(block))
			return true;
		else
			return false;
	}

	public static boolean isMHPowered(Block block) {
		return block.hasMetadata(MH_POWERED);
	}

	public static boolean isRedstoneWire(Block block) {
		if (block.getType().equals(Material.REDSTONE_WIRE))
			return true;
		else
			return false;
	}

	public static boolean isRedstoneLamp(Block block) {
		if (block.getType().equals(Material.matchMaterial("REDSTONE_LAMP_OFF"))
				|| block.getType().equals(Material.matchMaterial("REDSTONE_LAMP_ON")))
			return true;
		else
			return false;
	}

	public static boolean isPiston(Block block) {
		if (block.getType().equals(Material.matchMaterial("PISTON_BASE"))
				|| block.getType().equals(Material.matchMaterial("PISTON_EXTENSION"))
				|| block.getType().equals(Material.matchMaterial("PISTON_MOVING_PIECE"))
				|| block.getType().equals(Material.matchMaterial("PISTON_STICKY_BASE")))
			return true;
		else
			return false;
	}

	public static boolean isPistonBase(Block block) {
		if (block.getType().equals(Material.matchMaterial("PISTON_BASE"))
				|| block.getType().equals(Material.matchMaterial("PISTON_STICKY_BASE")))
			return true;
		else
			return false;
	}

	public static boolean isMHIndirectPoweredBySign(Block block) {
		for (BlockFace bf : possibleBlockface) {
			if (MasterMobHunterTools.isMHPoweredSign(block.getRelative(bf)))
				return true;
		}
		return false;
	}

}

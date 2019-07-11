package one.lindegaard.MobHunting.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import one.lindegaard.Core.Tools;
import one.lindegaard.Core.Materials.Materials;
import one.lindegaard.Core.Server.Servers;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.compatibility.CitizensCompat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

public class MasterMobHunterSign implements Listener {

	private MobHunting plugin;

	public final static String MH_SIGN = "MH:sign";
	public final static byte POWER_FROM_SIGN = 15;
	public final static String MH_POWERED = "MH:powered";

	// https://regex101.com/
	// Regex string="\[(MH|mh|Mh|mH)(\d+)(\+)?\]"
	// Example: [mh001+]
	final static String MASTERMOBHUNTERSIGN = "\\[(MH|mh|Mh|mH)(\\d+)(\\+)?\\]";

	public static List<Material> supportedmats = new ArrayList<Material>();

	public MasterMobHunterSign(MobHunting plugin) {
		this.plugin = plugin;

		supportedmats.add(Material.matchMaterial("REDSTONE_LAMP_OFF"));
		supportedmats.add(Material.matchMaterial("REDSTONE_LAMP_ON"));
		supportedmats.add(Material.matchMaterial("REDSTONE_LAMP"));
		supportedmats.add(Material.matchMaterial("LIT_REDSTONE_LAMP"));
		supportedmats.add(Material.matchMaterial("REDSTONE_TORCH_OFF"));
		supportedmats.add(Material.matchMaterial("REDSTONE_TORCH_ON"));
		supportedmats.add(Material.REDSTONE_WIRE);

	}

	public final static BlockFace possibleBlockface[] = new BlockFace[] { BlockFace.NORTH, BlockFace.EAST,
			BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN };

	// ****************************************************************************'
	// SETTERS
	// ****************************************************************************'

	public static void setPower(Block b, byte power) {
		if (isMHSign(b)) {
			if (isPowerSetOnSign(b))
				setMHPower(b, POWER_FROM_SIGN);
			else
				removeMHPower(b);
		} else if (MasterMobHunterRedstoneWire.isRedstoneWire(b)) {
			MasterMobHunterRedstoneWire.setPowerOnRedstoneWire(b, power);
		}
	}

	public static void removePower(Block block) {
		if (isMHPowered(block)) {
			block.removeMetadata(MH_POWERED, MobHunting.getInstance());
		}
		for (BlockFace bf : possibleBlockface) {
			Block rb = block.getRelative(bf);
			if (MasterMobHunterRedstoneLamp.isRedstoneLamp(rb)) {
				MasterMobHunterRedstoneLamp.removePowerFromredstoneLamp(rb);
			} else if (MasterMobHunterRedstoneWire.isRedstoneWire(rb)) {
				MasterMobHunterRedstoneWire.removePowerFromRedstoneWire(rb);
			} else if (MasterMobHunterPiston.isPistonBase(rb)) {
				MasterMobHunterPiston.removePowerOnPiston(rb);
			}
			removeMHPower(rb);
		}
	}

	// ****************************************************************************'

	private static void setMHPower(Block b, byte power) {
		if (power >= 0 && power <= POWER_FROM_SIGN) {
			b.setMetadata(MH_POWERED, new FixedMetadataValue(MobHunting.getInstance(), power));
			if (MasterMobHunterRedstoneWire.isRedstoneWire(b))
				MasterMobHunterRedstoneWire.setPowerOnRedstoneWire(b, power);
		}
		if (isMHSign(b) || isMHIndirectPoweredBySign(b)) {
			power = POWER_FROM_SIGN;
		} else {
			power--;
		}
		if (power >= 0 && power <= POWER_FROM_SIGN) {
			for (BlockFace bf : possibleBlockface) {
				Block rb = b.getRelative(bf);
				if (supportedmats.contains(rb.getType())) {
					if (isMHIndirectPoweredBySign(rb)) {
						if (MasterMobHunterRedstoneWire.isRedstoneWire(rb)) {
							MasterMobHunterRedstoneWire.setPowerOnRedstoneWire(rb, power);
						} else if (MasterMobHunterRedstoneLamp.isRedstoneLamp(rb)) {
							MasterMobHunterRedstoneLamp.setPowerOnRedstoneLamp(rb, power);
						} else if (MasterMobHunterPiston.isPistonBase(rb)) {
							MasterMobHunterPiston.setPowerOnPiston(rb);
						}
						if (!isMHPowered(rb))
							setMHPower(rb, power);
					}
				}
			}
		}
	}

	public static void removeMHPower(Block block) {
		block.removeMetadata(MH_POWERED, MobHunting.getInstance());
		for (BlockFace bf : possibleBlockface) {
			Block rb = block.getRelative(bf);
			if (isMHPowered(rb) && !isMHPoweredSign(rb))
				removeMHPower(rb);
		}
	}

	public static void setMHPowerLater(final Block block) {
		Bukkit.getScheduler().runTaskLater(MobHunting.getInstance(), new Runnable() {
			@Override
			public void run() {
				if (MasterMobHunterRedstoneLamp.isRedstoneLamp(block))
					MasterMobHunterRedstoneLamp.setPowerOnRedstoneLamp(block, POWER_FROM_SIGN);
				else if (MasterMobHunterPiston.isPistonBase(block))
					MasterMobHunterPiston.setPowerOnPiston(block);
				block.setMetadata(MH_POWERED, new FixedMetadataValue(MobHunting.getInstance(), POWER_FROM_SIGN));
			}
		}, 1L);
	}

	// ****************************************************************************'
	// Getters
	// ****************************************************************************'

	public static int getNPCIdOnSign(Block block) {
		if (!Materials.isSign(block))
			return -1;

		String str = ((org.bukkit.block.Sign) block.getState()).getLine(0);
		return getNPCIdOnSign(str);
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

	// ****************************************************************************'
	// Events
	// ****************************************************************************'

	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
		if (event.isCancelled())
			return;

		if (event.getClickedBlock() == null)
			return;

		if (event.getPlayer().getItemInHand().getType().equals(Material.STICK)) {

			boolean turnon = (event.getAction() == Action.LEFT_CLICK_BLOCK);

			// Check if Block is powered or indirectly powered
			int power = 0;
			if (event.getClickedBlock().hasMetadata(MH_POWERED)) {
				for (MetadataValue mdv : event.getClickedBlock().getMetadata(MH_POWERED)) {
					int p = mdv.asInt();
					power = power > p ? power : p;
				}
			}

			MobHunting.getInstance().getMessages().debug("%s hasMeta(MH_POWERED)=%s, power=%s",
					event.getClickedBlock().getType(), event.getClickedBlock().hasMetadata(MH_POWERED), power);

			// Check if block is MMH Sign
			if (isMHSign(event.getClickedBlock())) {
				if (event.getPlayer().getItemInHand().getType().equals(Material.STICK)) {
					int id = getNPCIdOnSign(event.getClickedBlock());
					if (id != -1) {
						if (power > 0)
							plugin.getMessages().playerActionBarMessageQueue(event.getPlayer(), MobHunting.getInstance()
									.getMessages().getString("mobhunting.npc.clickednpcsignpowered", "npcid", id));
						else
							plugin.getMessages().playerActionBarMessageQueue(event.getPlayer(), MobHunting.getInstance()
									.getMessages().getString("mobhunting.npc.clickednpcsign", "npcid", id));

						NPC npc = CitizensAPI.getNPCRegistry().getById(id);
						if (npc != null) {
							if (CitizensCompat.getMasterMobHunterManager().isMasterMobHunter(npc)) {

								MasterMobHunter mmh = CitizensCompat.getMasterMobHunterManager().get(npc.getId());

								if (isMHSign(((org.bukkit.block.Sign) event.getClickedBlock().getState()).getLine(0))) {
									event.getClickedBlock().setMetadata(MH_SIGN, new FixedMetadataValue(
											MobHunting.getInstance(),
											((org.bukkit.block.Sign) event.getClickedBlock().getState()).getLine(0)));
									((org.bukkit.block.Sign) event.getClickedBlock().getState()).setMetadata(MH_SIGN,
											new FixedMetadataValue(MobHunting.getInstance(),
													((org.bukkit.block.Sign) event.getClickedBlock().getState())
															.getLine(0)));
									mmh.putSignLocation(event.getClickedBlock().getLocation());
									CitizensCompat.getMasterMobHunterManager().put(id, mmh);
								}
								((org.bukkit.block.Sign) event.getClickedBlock().getState()).setLine(1,
										Tools.trimSignText(mmh.getRank() + "." + npc.getName()));
								((org.bukkit.block.Sign) event.getClickedBlock().getState()).setLine(2,
										Tools.trimSignText(mmh.getPeriod().translateNameFriendly()));
								((org.bukkit.block.Sign) event.getClickedBlock().getState()).setLine(3,
										Tools.trimSignText(
												mmh.getNumberOfKills() + " " + mmh.getStatType().translateName()));

								if (turnon) {
									setPower(event.getClickedBlock(), MasterMobHunterSign.POWER_FROM_SIGN);
								} else {
									boolean powered = isPowerSetOnSign(event.getClickedBlock());
									if (powered) {
										OfflinePlayer offlinePlayer = Bukkit.getPlayer(npc.getName());
										if (offlinePlayer != null && offlinePlayer.isOnline()) {
											setPower(event.getClickedBlock(), MasterMobHunterSign.POWER_FROM_SIGN);
										} else {
											removePower(event.getClickedBlock());
										}
									} else
										removePower(event.getClickedBlock());
									CitizensCompat.getMasterMobHunterManager().get(npc.getId()).update();
								}
							}
						}
					} else {
						((org.bukkit.block.Sign) event.getClickedBlock().getState()).setLine(1,
								"Id=" + id + " is not a");
						((org.bukkit.block.Sign) event.getClickedBlock().getState()).setLine(2, "MasterMobHunter");
						((org.bukkit.block.Sign) event.getClickedBlock().getState()).setLine(3, "");
						((org.bukkit.block.Sign) event.getClickedBlock().getState()).update();
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockPlaceEvent(BlockPlaceEvent e) {
		// BlockPlaceEvent is called before the player enter the text on the
		// sign

		Block b = e.getBlock();
		if (MasterMobHunterRedstoneWire.isRedstoneWire(b)) {
			if (isMHIndirectPoweredBySign(b)) {
				MasterMobHunterRedstoneWire.setPowerOnRedstoneWire(b, POWER_FROM_SIGN);
				// power on Redstone must be set immediately to work
				// setMHPower(b, POWER_FROM_SIGN);
				// b.getState().setRawData(POWER_FROM_SIGN);
				// b.getState().update(true, false);
			}
		} else if ((MasterMobHunterRedstoneLamp.isRedstoneLamp(b) || MasterMobHunterPiston.isPistonBase(b))
				&& isMHIndirectPoweredBySign(b)) {
			// power on Redstone Lamp and Piston must be set in next tick to
			// work
			setMHPowerLater(b);
		}

	}

	@EventHandler
	public void onSignChangeEvent(SignChangeEvent event) {

		Player player = event.getPlayer();
		Block sb = event.getBlock();
		if (isMHSign(sb) || isMHSign(event.getLine(0))) {
			int id = getNPCIdOnSign(event.getLine(0));
			if (id != -1) {
				boolean powered = isPowerSetOnSign(sb);
				NPC npc = CitizensAPI.getNPCRegistry().getById(id);
				if (npc != null) {
					if (CitizensCompat.getMasterMobHunterManager().isMasterMobHunter(npc)) {
						MasterMobHunter mmh = CitizensCompat.getMasterMobHunterManager().get(npc.getId());

						if (isMHSign(event.getLine(0))) {
							sb.setMetadata(MH_SIGN, new FixedMetadataValue(MobHunting.getInstance(), event.getLine(0)));
							((org.bukkit.block.Sign) sb.getState()).setMetadata(MH_SIGN, new FixedMetadataValue(
									MobHunting.getInstance(), ((org.bukkit.block.Sign) sb.getState()).getLine(0)));
							mmh.putSignLocation(sb.getLocation());
							CitizensCompat.getMasterMobHunterManager().put(id, mmh);
							plugin.getMessages().playerActionBarMessageQueue(player,
									player.getName() + " placed a MobHunting Sign (ID=" + id + ")");
						}

						event.setLine(1, Tools.trimSignText(mmh.getRank() + "." + npc.getName()));
						event.setLine(2, Tools.trimSignText(mmh.getPeriod().translateNameFriendly()));
						event.setLine(3,
								Tools.trimSignText(mmh.getNumberOfKills() + " " + mmh.getStatType().translateName()));

						if (powered) {
							OfflinePlayer offlinePlayer = Bukkit.getPlayer(npc.getName());
							if (offlinePlayer != null && offlinePlayer.isOnline() && !CitizensCompat.isNPC(player))
								setPower(event.getBlock(), MasterMobHunterSign.POWER_FROM_SIGN);
							else
								removePower(event.getBlock());
						} else
							removePower(event.getBlock());
						CitizensCompat.getMasterMobHunterManager().get(npc.getId()).update();
					} else {
						event.setLine(1, "ID=" + id + " is not a");
						event.setLine(2, "MasterMobHunter");
						event.setLine(3, "");
					}
				} else {
					event.setLine(1, "Invalid npc id");
					event.setLine(2, "");
					event.setLine(3, "");
				}
			} else {
				MobHunting.getInstance().getMessages().debug("The sign does not have a valid NPC id!(%s)", id);
			}
		}

	}

	// ************************************************************************************
	// TESTS
	// ************************************************************************************

	public static boolean isPowerSetOnSign(Block block) {
		String str;
		if (isMHSign(block)) {
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

	public static boolean isMHSign(Block block) {
		if (Materials.isSign(block)) {
			org.bukkit.block.Sign sign;
			if (Servers.isMC113OrNewer()) {
				sign = (org.bukkit.block.Sign) block.getState();
			} else {
				if (block.getState() instanceof org.bukkit.block.Sign) {
					sign = (org.bukkit.block.Sign) block.getState();
				} else
					return false;
			}

			if (sign.getLine(0).matches(MASTERMOBHUNTERSIGN)) {
				return true;
			} else if (block.hasMetadata(MH_SIGN)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isMHSign(String line) {
		return line.matches(MASTERMOBHUNTERSIGN);
	}

	public static boolean isMHPoweredSign(Block block) {
		if (isMHSign(block) && isMHPowered(block))
			return true;
		else
			return false;
	}

	public static boolean isMHPowered(Block block) {
		return block.hasMetadata(MH_POWERED);
	}

	public static boolean isMHIndirectPoweredBySign(Block block) {
		for (BlockFace bf : possibleBlockface) {
			if (isMHPoweredSign(block.getRelative(bf)))
				return true;
		}
		return false;
	}
}

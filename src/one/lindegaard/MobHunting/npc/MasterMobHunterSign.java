package one.lindegaard.MobHunting.npc;

import java.util.ArrayList;
import java.util.List;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import one.lindegaard.Core.Tools;
import one.lindegaard.Core.Server.Servers;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.compatibility.CitizensCompat;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.PistonBaseMaterial;
import org.bukkit.material.PistonExtensionMaterial;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

public class MasterMobHunterSign implements Listener {

	private MobHunting plugin;

	public final static String MH_SIGN = "MH:sign";
	public final static byte POWER_FROM_SIGN = 15;
	private final static String MH_POWERED = "MH:powered";

	// https://regex101.com/
	// Regex string="\[(MH|mh|Mh|mH)(\d+)(\+)?\]"
	// Example: [mh001+]
	final static String MASTERMOBHUNTERSIGN = "\\[(MH|mh|Mh|mH)(\\d+)(\\+)?\\]";

	public static List<Material> supportedmats = new ArrayList<Material>();

	public MasterMobHunterSign(MobHunting plugin) {
		this.plugin = plugin;

		supportedmats.add(Material.matchMaterial("REDSTONE_LAMP_OFF"));
		supportedmats.add(Material.matchMaterial("REDSTONE_LAMP_ON"));
		supportedmats.add(Material.matchMaterial("REDSTONE_TORCH_OFF"));
		supportedmats.add(Material.matchMaterial("REDSTONE_TORCH_ON"));
		supportedmats.add(Material.REDSTONE_WIRE);

	}

	private final static BlockFace possibleBlockface[] = new BlockFace[] { BlockFace.NORTH, BlockFace.EAST,
			BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN };

	// ****************************************************************************'
	// SETTERS
	// ****************************************************************************'

	public static void setPower(Block b, byte power) {
		if (MasterMobHunterTools.isMHSign(b)) {
			if (MasterMobHunterTools.isPowerSetOnSign(b))
				setMHPower(b, POWER_FROM_SIGN);
			else
				MasterMobHunterTools.removeMHPower(b);
		} else if (MasterMobHunterTools.isRedstoneWire(b)) {
			setMHPowerOnRedstoneWire(b, power);
		}
	}

	private static void setMHPower(Block b, byte power) {
		if (power >= 0 && power <= POWER_FROM_SIGN) {
			b.setMetadata(MH_POWERED, new FixedMetadataValue(MobHunting.getInstance(), power));
			if (MasterMobHunterTools.isRedstoneWire(b))
				setMHPowerOnRedstoneWire(b, power);
		}
		if (MasterMobHunterTools.isMHSign(b) || MasterMobHunterTools.isMHIndirectPoweredBySign(b)) {
			power = POWER_FROM_SIGN;
		} else {
			power--;
		}
		if (power >= 0 && power <= POWER_FROM_SIGN) {
			for (BlockFace bf : possibleBlockface) {
				Block rb = b.getRelative(bf);
				if (supportedmats.contains(rb.getType())) {
					if (MasterMobHunterTools.isMHIndirectPoweredBySign(rb)) {
						if (MasterMobHunterTools.isRedstoneWire(rb)) {
							setMHPowerOnRedstoneWire(rb, power);
						} else if (MasterMobHunterTools.isRedstoneLamp(rb)) {
							setPowerOnRedstoneLamp(rb, power);
						} else if (MasterMobHunterTools.isPistonBase(rb)) {
							setPowerOnPiston(rb);
						}
						if (!MasterMobHunterTools.isMHPowered(rb))
							setMHPower(rb, power);
					}
				}
			}
		}
	}

	public static void setMHPowerOnRedstoneWire(Block block, byte power) {
		if (Servers.isMC113OrNewer()) {
			// org.bukkit.block.data.type.RedstoneWire rw =
			// (org.bukkit.block.data.type.RedstoneWire) block.getBlockData();
			// rw.setPower(0);
			// block.setBlockData(rw);
		} else {

			// BlockData bd = block.getBlockData();
			// BlockState bs = block.getState();
			// byte br = bs.getRawData();
			// bs.setRawData(power);
			// String bd_Str = bd.getAsString();
			// block.setBlockData(bd);
			// block.getState().setRawData(power);
			// block.setBlockData(bd,true);
			// block.getState().update(true,false);

			block.setType(Material.matchMaterial("REDSTONE_WIRE"));
			block.getState().setRawData(power);
			block.getState().update();
		}
	}

	public static void setPowerOnRedstoneLamp(Block lamp, byte power) {
		if (lamp.getType().equals(Material.matchMaterial("REDSTONE_LAMP_OFF"))
				&& MasterMobHunterTools.isMHIndirectPoweredBySign(lamp)) {
			for (BlockFace bf : possibleBlockface) {
				Block rb = lamp.getRelative(bf);
				if (MasterMobHunterTools.isMHPoweredSign(rb)) {
					Bukkit.getConsoleSender().sendMessage("MASTERMOBHUNTERSIGN: 2222");

					// Material signType = rb.getType();
					Sign sign = (Sign) rb.getState();
					// MaterialData md = sign.getData();
					String[] copyOfSigntext = sign.getLines();
					rb.setType(Material.matchMaterial("REDSTONE_TORCH_ON"));
					// rb.setTypeIdAndData(signType.getId(), md.getData(), false);
					lamp.getState().setRawData(power);
					Sign newSign = ((Sign) rb.getState());
					for (int i = 0; i < 4; i++) {
						newSign.setLine(i, copyOfSigntext[i]);
					}
					newSign.update(true, false);
				}
			}
		}
	}

	public static void setPowerOnPiston(Block b) {
		PistonBaseMaterial pistonData = (PistonBaseMaterial) b.getState().getData();
		if (!pistonData.isPowered()) {
			pistonData.setPowered(true);

			b.getState().setRawData(pistonData.getData());
			b.getState().update();

			BlockFace blockFace = pistonData.getFacing();
			Block tb = b.getRelative(blockFace);
			tb.setType(Material.matchMaterial("PISTON_EXTENSION"), false);
			PistonExtensionMaterial pistonExtentionData = (PistonExtensionMaterial) tb.getState().getData();
			pistonExtentionData.setFacingDirection(b.getFace(tb));
			tb.getState().setRawData(pistonExtentionData.getData());
			tb.getState().update(true, false);
		}
	}

	// ****************************************************************************'
	// GETTERS
	// ****************************************************************************'

	// ****************************************************************************'
	// REMOVE
	// ****************************************************************************'

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

			MobHunting.getInstance().getMessages().debug("power=%s, hasMeta(MH_POWERED)=%s", power,
					event.getClickedBlock().hasMetadata(MH_POWERED));

			// Check if block is MMH Sign
			if (MasterMobHunterTools.isMHSign(event.getClickedBlock())) {
				if (event.getPlayer().getItemInHand().getType().equals(Material.STICK)) {
					int id = MasterMobHunterTools.getNPCIdOnSign(event.getClickedBlock());
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

								if (MasterMobHunterTools.isMHSign(
										((org.bukkit.block.Sign) event.getClickedBlock().getState()).getLine(0))) {
									event.getClickedBlock().setMetadata(MH_SIGN, new FixedMetadataValue(
											MobHunting.getInstance(),
											((org.bukkit.block.Sign) event.getClickedBlock().getState()).getLine(0)));
									((Sign) event.getClickedBlock().getState()).setMetadata(MH_SIGN,
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
									boolean powered = MasterMobHunterTools.isPowerSetOnSign(event.getClickedBlock());
									if (powered) {
										OfflinePlayer offlinePlayer = Bukkit.getPlayer(npc.getName());
										if (offlinePlayer != null && offlinePlayer.isOnline()) {
											setPower(event.getClickedBlock(), MasterMobHunterSign.POWER_FROM_SIGN);
										} else {
											MasterMobHunterTools.removePower(event.getClickedBlock());
										}
									} else
										MasterMobHunterTools.removePower(event.getClickedBlock());
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

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockPlaceEvent(BlockPlaceEvent e) {
		// BlockPlaceEvent is called before the player enter the text on the
		// sign

		Block b = e.getBlock();
		if (MasterMobHunterTools.isRedstoneWire(b)) {
			if (MasterMobHunterTools.isMHIndirectPoweredBySign(b)) {
				// power on Redstone must be set immediately to work
				setMHPower(b, POWER_FROM_SIGN);
				b.getState().setRawData(POWER_FROM_SIGN);
				b.getState().update(true, false);
			}
		} else if ((MasterMobHunterTools.isRedstoneLamp(b) || MasterMobHunterTools.isPistonBase(b))
				&& MasterMobHunterTools.isMHIndirectPoweredBySign(b)) {
			// power on Redstone Lamp and Piston must be set in next tick to
			// work
			setMHPowerLater(b);
		}

	}

	@EventHandler
	public void onSignChangeEvent(SignChangeEvent event) {

		Player player = event.getPlayer();
		Block sb = event.getBlock();
		if (MasterMobHunterTools.isMHSign(sb) || MasterMobHunterTools.isMHSign(event.getLine(0))) {
			int id = MasterMobHunterTools.getNPCIdOnSign(event.getLine(0));
			if (id != -1) {
				boolean powered = MasterMobHunterTools.isPowerSetOnSign(sb);
				NPC npc = CitizensAPI.getNPCRegistry().getById(id);
				if (npc != null) {
					if (CitizensCompat.getMasterMobHunterManager().isMasterMobHunter(npc)) {
						MasterMobHunter mmh = CitizensCompat.getMasterMobHunterManager().get(npc.getId());

						if (MasterMobHunterTools.isMHSign(event.getLine(0))) {
							sb.setMetadata(MH_SIGN, new FixedMetadataValue(MobHunting.getInstance(), event.getLine(0)));
							((Sign) sb.getState()).setMetadata(MH_SIGN, new FixedMetadataValue(MobHunting.getInstance(),
									((org.bukkit.block.Sign) sb.getState()).getLine(0)));
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
								MasterMobHunterTools.removePower(event.getBlock());
						} else
							MasterMobHunterTools.removePower(event.getBlock());
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

	public static void setMHPowerLater(final Block block) {
		Bukkit.getScheduler().runTaskLater(MobHunting.getInstance(), new Runnable() {
			@Override
			public void run() {
				if (MasterMobHunterTools.isRedstoneLamp(block))
					setPowerOnRedstoneLamp(block, POWER_FROM_SIGN);
				else if (MasterMobHunterTools.isPistonBase(block))
					setPowerOnPiston(block);
				block.setMetadata(MH_POWERED, new FixedMetadataValue(MobHunting.getInstance(), (byte) 15));
			}
		}, 1L);
	}

	// ************************************************************************************
	// TESTS
	// ************************************************************************************

}

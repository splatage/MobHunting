package one.lindegaard.MobHunting.npc;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.metadata.MetadataValue;

import one.lindegaard.MobHunting.compatibility.CitizensCompat;

public class MasterMobHunterEvents implements Listener{

	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockRedstoneEvent(BlockRedstoneEvent e) {
		Block b = e.getBlock();
		if (MasterMobHunterTools.isMHPowered(b)) {
			for (MetadataValue mdv : b.getMetadata(MasterMobHunterTools.MH_POWERED)) {
				if (MasterMobHunterTools.isMHIndirectPoweredBySign(e.getBlock()))
					e.setNewCurrent(mdv.asInt());
				else {
					MasterMobHunterTools.removeMHPower(b);
					e.setNewCurrent(0);
				}
				if (mdv.asInt() == 0) {
					MasterMobHunterTools.removeMHPower(b);
					for (BlockFace bf : MasterMobHunterTools.possibleBlockface) {
						Block rb = b.getRelative(bf);
						MasterMobHunterTools.removeMHPower(rb);
					}
				}
			}
		}
	}

	@EventHandler
	public void onBlockPhysicsEvent(final BlockPhysicsEvent e) {

		// This is the block which is going to be changed
		Block b = e.getBlock();
		// getChangedType() the type of block that changed, causing this
		// event
		Material c = e.getChangedType();

		if (b == null || b.getType() == null)
			return;

		//if (b.getType().equals(Material.LEGACY_REDSTONE_LAMP_ON)) {
		if (b.getType().equals(Material.matchMaterial("REDSTONE_LAMP_ON"))) {
			if (MasterMobHunterTools.isMHIndirectPoweredBySign(b)) {
				e.setCancelled(true);
			}
		} else if (MasterMobHunterTools.isPiston(b)) {
			if (MasterMobHunterTools.isMHIndirectPoweredBySign(b))
				if (MasterMobHunterTools.isMHPowered(b)) {
					e.setCancelled(true);
					MasterMobHunterSign.setMHPowerLater(b);
				}
			if ((b.getType().equals(Material.matchMaterial("PISTON_EXTENSION")) && c.equals(Material.REDSTONE_WIRE))) {

			}
		}
	}

	@EventHandler
	public void onBlockBreakEvent(BlockBreakEvent e) {
		Block b = e.getBlock();
		if (MasterMobHunterTools.isMHPowered(b)) {
			MasterMobHunterTools.removeMHPower(b);
		}
		if (MasterMobHunterTools.isMHSign(b)) {
			int id = MasterMobHunterTools.getNPCIdOnSign(b);
			if (id != -1 && CitizensCompat.getMasterMobHunterManager().get(id) != null) {
				CitizensCompat.getMasterMobHunterManager().get(id).removeLocation(e.getBlock().getLocation());
			}
		}
	}

	
}

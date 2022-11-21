package one.lindegaard.MobHunting.rewards;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

import one.lindegaard.CustomItemsLib.server.Servers;

public class EntityPickupItemEventListener implements Listener {

	// TODO: must be moved to Core

	private PickupRewards pickupRewards;

	public EntityPickupItemEventListener(PickupRewards pickupRewards) {
		this.pickupRewards = pickupRewards;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onEntityPickupItemEvent(EntityPickupItemEvent event) {
		// OBS: EntityPickupItemEvent does only exist in MC1.12 and newer

		// This event is NOT called when the inventory is full.
		if (event.isCancelled())
			return;

		Entity entity = event.getEntity();

		if (event.getEntity().getType() != EntityType.PLAYER) {
			// Entity is not a Player
			if (entity.getType().equals(EntityType.ZOMBIE) || entity.getType().equals(EntityType.SKELETON)
					|| entity.getType().equals(EntityType.WITHER_SKELETON)
					|| (Servers.isMC116OrNewer()) && entity.getType().equals(EntityType.ZOMBIFIED_PIGLIN)) {
				event.setCancelled(true);
			}
			return;
		}
		if (((Player) entity).getInventory().firstEmpty() != -1)
			pickupRewards.rewardPlayer((Player) entity, event.getItem(), event::setCancelled);
		
	}

}

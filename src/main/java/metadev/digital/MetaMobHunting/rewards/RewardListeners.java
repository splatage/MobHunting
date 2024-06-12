package metadev.digital.MetaMobHunting.rewards;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.block.Action;

import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.materials.Materials;
import metadev.digital.metacustomitemslib.rewards.Reward;
import metadev.digital.metacustomitemslib.server.Servers;
import metadev.digital.MetaMobHunting.MobHunting;

public class RewardListeners implements Listener {

	private MobHunting plugin;

	public RewardListeners(MobHunting plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
		if (event.isCancelled())
			return;

		if (event.getClickedBlock() == null)
			return;

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;

		if (Servers.isMC19OrNewer() && event.getHand() != EquipmentSlot.HAND)
			return;

		Player player = event.getPlayer();

		Block block = event.getClickedBlock();

		if (Reward.isReward(block)) {
			Reward reward = Reward.getReward(block);
			if (reward.getMoney() == 0)
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(Core.getConfigManager().rewardTextColor) + reward.getDisplayName());
			else
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(Core.getConfigManager().rewardTextColor)
								+ (Core.getConfigManager().rewardItemtype.equalsIgnoreCase("ITEM")
										? plugin.getRewardManager().format(reward.getMoney())
										: reward.getDisplayName() + " ("
												+ plugin.getRewardManager().format(reward.getMoney()) + ")"));
		} else if (Servers.isMC113OrNewer()
				&& (block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD)) {
			Skull skullState = (Skull) block.getState();
			OfflinePlayer owner = skullState.getOwningPlayer();
			if (owner != null && owner.getName() != null)
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(Core.getConfigManager().rewardTextColor) + owner.getName());
		} else if (Materials.isSkull(block.getType())) {
			Skull skullState = (Skull) block.getState();
			switch (skullState.getSkullType()) {
			case PLAYER:
				if (Servers.isMC19OrNewer()) {
					OfflinePlayer owner = skullState.getOwningPlayer();
					if (owner != null && owner.getName() != null) {
						plugin.getMessages().playerActionBarMessageQueue(player,
								ChatColor.valueOf(Core.getConfigManager().rewardTextColor) + owner.getName());
					} else
						plugin.getMessages().playerActionBarMessageQueue(player,
								ChatColor.valueOf(Core.getConfigManager().rewardTextColor)
										+ plugin.getMessages().getString("mobhunting.reward.customtexture"));
				} else if (skullState.hasOwner()) {
					@SuppressWarnings("deprecation")
					String owner = skullState.getOwner();
					if (owner != null && !owner.equalsIgnoreCase("")) {
						plugin.getMessages().playerActionBarMessageQueue(player,
								ChatColor.valueOf(Core.getConfigManager().rewardTextColor) + owner);
					} else
						plugin.getMessages().playerActionBarMessageQueue(player,
								ChatColor.valueOf(Core.getConfigManager().rewardTextColor)
										+ plugin.getMessages().getString("mobhunting.reward.customtexture"));
				} else
					plugin.getMessages().playerActionBarMessageQueue(player,
							ChatColor.valueOf(Core.getConfigManager().rewardTextColor)
									+ plugin.getMessages().getString("mobhunting.reward.steve"));
				break;
			case CREEPER:
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(Core.getConfigManager().rewardTextColor)
								+ plugin.getMessages().getString("mobs.Creeper.name"));
				break;
			case SKELETON:
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(Core.getConfigManager().rewardTextColor)
								+ plugin.getMessages().getString("mobs.Skeleton.name"));
				break;
			case WITHER:
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(Core.getConfigManager().rewardTextColor)
								+ plugin.getMessages().getString("mobs.Wither.name"));
				break;
			case ZOMBIE:
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(Core.getConfigManager().rewardTextColor)
								+ plugin.getMessages().getString("mobs.Zombie.name"));
				break;
			case DRAGON:
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(Core.getConfigManager().rewardTextColor)
								+ plugin.getMessages().getString("mobs.EnderDragon.name"));
				break;
			default:
				break;
			}
		}
	}

}

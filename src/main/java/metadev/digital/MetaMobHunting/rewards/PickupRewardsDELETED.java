/** // TODO: POSSIBLY DEPRECATED package metadev.digital.MetaMobHunting.rewards;

import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.Tools;
import metadev.digital.metacustomitemslib.rewards.Reward;
import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.compatibility.BagOfGoldCompat;
import metadev.digital.MetaMobHunting.compatibility.ProtocolLibCompat;
import metadev.digital.MetaMobHunting.compatibility.ProtocolLibHelper;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

public class PickupRewardsDELETED {

	private MobHunting plugin;

	public PickupRewardsDELETED(MobHunting plugin) {
		this.plugin = plugin;
	}

	public void rewardPlayer(Player player, Item item, CallBack callBack) {
		if (Reward.isReward(item)) {
			boolean succes = false;
			Reward reward = Reward.getReward(item);
			if (reward.isMoney()) {
				callBack.setCancelled(true);
				if (player.getGameMode() == GameMode.SPECTATOR) {
					return;
				} else if (BagOfGoldCompat.isSupported()) {
					succes = plugin.getEconomyManager().depositPlayer(player, reward.getMoney());
				} else if (reward.getMoney() != 0 && !plugin.getConfigManager().dropMoneyOnGroundUseItemAsCurrency) {
					// If not Gringotts
					succes = plugin.getEconomyManager().depositPlayer(player, reward.getMoney());
				} else {
					succes = plugin.getRewardManager().addBagOfGoldPlayer(player, reward.getMoney()) > 0;
				}
			} else if (reward.isKilledHeadReward() || reward.isKillerHeadReward()) {
				plugin.getMessages().debug("Merge MobHunting heads on pickup is still not implemented");
				// plugin.getMessages().debug("%s collected %s to the cursor", player.getName(),
				// saldo);
			}
			if (succes) {
				item.remove();
				if (Core.getCoreRewardManager().getDroppedMoney().containsKey(item.getEntityId()))
					Core.getCoreRewardManager().getDroppedMoney().remove(item.getEntityId());
				if (ProtocolLibCompat.isSupported())
					ProtocolLibHelper.pickupMoney(player, item);

				if (reward.getMoney() == 0) {
					plugin.getMessages()
							.debug("%s picked up a %s" + ChatColor.RESET + " (# of rewards left=%s)", player.getName(),
									Core.getConfigManager().rewardItemtype.equalsIgnoreCase("ITEM") ? "ITEM"
											: reward.getDisplayName(),
									Core.getCoreRewardManager().getDroppedMoney().size());
				} else {
					plugin.getMessages().debug(
							"%s picked up a %s" + ChatColor.RESET + " with a value:%s (# of rewards left=%s)(PickupRewards)", player.getName(),
							reward.getDisplayName().equalsIgnoreCase("") ? reward.getDisplayName()
									: Core.getConfigManager().rewardItemtype,
							plugin.getRewardManager().format(Tools.round(reward.getMoney())),
							Core.getCoreRewardManager().getDroppedMoney().size());
					plugin.getMessages().playerActionBarMessageQueue(player,
							plugin.getMessages().getString("mobhunting.moneypickup", "money",
									plugin.getRewardManager().format(reward.getMoney()), "rewardname",
									ChatColor.valueOf(Core.getConfigManager().rewardTextColor)
											+ (reward.getDisplayName().isEmpty() ? Core.getConfigManager().bagOfGoldName
													: reward.getDisplayName())));

				}
			}
		}
	}

	public interface CallBack {
		void setCancelled(boolean canceled);
	}

}
*/
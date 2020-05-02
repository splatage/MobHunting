package one.lindegaard.MobHunting.rewards;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;
import org.bukkit.event.block.Action;

import one.lindegaard.Core.Materials.Materials;
import one.lindegaard.Core.Server.Servers;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.compatibility.BagOfGoldCompat;
import one.lindegaard.MobHunting.compatibility.CitizensCompat;
import one.lindegaard.MobHunting.compatibility.ProtocolLibCompat;
import one.lindegaard.MobHunting.compatibility.ProtocolLibHelper;
import one.lindegaard.MobHunting.util.Misc;

public class RewardListeners implements Listener {

	private MobHunting plugin;

	public RewardListeners(MobHunting plugin) {
		this.plugin = plugin;
	}

	private boolean isFakeReward(Item item) {
		ItemStack itemStack = item.getItemStack();
		return isFakeReward(itemStack);
	}

	private boolean isFakeReward(ItemStack itemStack) {

		if (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()
				&& itemStack.getItemMeta().getDisplayName()
						.contains(MobHunting.getAPI().getConfigManager().dropMoneyOnGroundSkullRewardName)) {
			if (!itemStack.getItemMeta().hasLore()) {
				return true;
			}
		}
		return false;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDropReward(PlayerDropItemEvent event) {
		if (event.isCancelled())
			return;

		Item item = event.getItemDrop();
		Player player = event.getPlayer();

		if (isFakeReward(item)) {
			player.sendMessage(ChatColor.RED + "[MobHunting] WARNING, this was a FAKE reward with no value");
			return;
		}

		if (Reward.isReward(item)) {
			Reward reward = Reward.getReward(item);
			double money = reward.getMoney();
			if (money == 0) {
				item.setCustomName(ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
						+ reward.getDisplayName());
				plugin.getRewardManager().getDroppedMoney().put(item.getEntityId(), money);
				plugin.getMessages().debug("%s dropped a %s (# of rewards left=%s)", player.getName(),
						reward.getDisplayName() != null ? reward.getDisplayName()
								: plugin.getConfigManager().dropMoneyOnGroundSkullRewardName.trim(),
						plugin.getRewardManager().getDroppedMoney().size());
			} else {
				if (reward.isItemReward())
					item.setCustomName(ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
							+ plugin.getRewardManager().format(money));
				else
					item.setCustomName(ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
							+ reward.getDisplayName() + " (" + plugin.getRewardManager().format(money) + ")");

				plugin.getRewardManager().getDroppedMoney().put(item.getEntityId(), money);
				if (!BagOfGoldCompat.isSupported() && !plugin.getConfigManager().dropMoneyOnGroup
						&& !plugin.getConfigManager().dropMoneyOnGroundUseItemAsCurrency)
					plugin.getEconomyManager().withdrawPlayer(player, money);

				plugin.getMessages().debug("%s dropped %s money. (# of rewards left=%s)", player.getName(),
						plugin.getRewardManager().format(money), plugin.getRewardManager().getDroppedMoney().size());
				plugin.getMessages().playerActionBarMessageQueue(player, plugin.getMessages().getString(
						"mobhunting.moneydrop", "money", plugin.getRewardManager().format(money), "rewardname",
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor) + (reward.isItemReward()
								? plugin.getConfigManager().dropMoneyOnGroundSkullRewardName.trim()
								: reward.getDisplayName())));
			}
			item.setCustomNameVisible(true);
			item.setMetadata(Reward.MH_REWARD_DATA, new FixedMetadataValue(plugin, reward));
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onDespawnRewardEvent(ItemDespawnEvent event) {
		if (event.isCancelled())
			return;

		if (BagOfGoldCompat.isSupported() && BagOfGoldCompat.useAsEconomyAnEconomyPlugin())
			return;

		if (Reward.isReward(event.getEntity())) {
			if (plugin.getRewardManager().getDroppedMoney().containsKey(event.getEntity().getEntityId())) {
				plugin.getRewardManager().getDroppedMoney().remove(event.getEntity().getEntityId());
				if (event.getEntity().getLastDamageCause() != null)
					plugin.getMessages().debug("The reward was destroyed by %s",
							event.getEntity().getLastDamageCause().getCause());
				else
					plugin.getMessages().debug("The reward despawned (# of rewards left=%s)",
							plugin.getRewardManager().getDroppedMoney().size());
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryPickupRewardEvent(InventoryPickupItemEvent event) {
		if (event.isCancelled())
			return;

		if (BagOfGoldCompat.isSupported() && BagOfGoldCompat.useAsEconomyAnEconomyPlugin())
			return;

		Item item = event.getItem();
		if (!item.hasMetadata(Reward.MH_REWARD_DATA))
			return;

		if (plugin.getConfigManager().denyHoppersToPickUpMoney
				&& event.getInventory().getType() == InventoryType.HOPPER) {
			// plugin.getMessages().debug("A %s tried to pick up the the reward,
			// but this is
			// disabled in config.yml",
			// event.getInventory().getType());
			event.setCancelled(true);
		} else {
			// plugin.getMessages().debug("The reward was picked up by %s",
			// event.getInventory().getType());
			if (plugin.getRewardManager().getDroppedMoney().containsKey(item.getEntityId()))
				plugin.getRewardManager().getDroppedMoney().remove(item.getEntityId());
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerMoveOverRewardEvent(PlayerMoveEvent event) {
		if (event.isCancelled())
			return;

		Player player = event.getPlayer();

		if (player.getInventory().firstEmpty() != -1)
			return;

		// Its not allowed to pickup BagOfGold in Spectator mode
		if (player.getGameMode() == GameMode.SPECTATOR)
			return;

		if (plugin.getRewardManager().canPickupMoney(player)) {

			Iterator<Entity> entityList = ((Entity) player).getNearbyEntities(1, 1, 1).iterator();
			while (entityList.hasNext() && plugin.getRewardManager().canPickupMoney(player)) {
				Entity entity = entityList.next();
				if (!(entity instanceof Item))
					continue;

				Item item = (Item) entity;

				if (isFakeReward(item)) {
					player.sendMessage(ChatColor.RED + "[MobHunting] WARNING, this was a FAKE reward with no value");
					return;
				}

				if (Reward.isReward(item)) {
					if (plugin.getRewardManager().getDroppedMoney().containsKey(entity.getEntityId())) {
						Reward reward = Reward.getReward(item);
						if (reward.isMoney()) {
							double addedMoney = 0;
							if (reward.getMoney() != 0 && !BagOfGoldCompat.isSupported()
									&& !plugin.getConfigManager().dropMoneyOnGroundUseItemAsCurrency) {

								boolean succes = plugin.getEconomyManager().depositPlayer(player, reward.getMoney());
								addedMoney = reward.getMoney();
								// addedMoney = plugin.getRewardManager().depositPlayer(player,
								// reward.getMoney()).amount;

							} else {
								// Inventory is full , check if item is
								// inventory
								for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
									ItemStack is = player.getInventory().getItem(slot);
									if (Reward.isReward(is)) {
										Reward rewardInSlot = Reward.getReward(is);
										if ((rewardInSlot.isMoney())) {
											rewardInSlot.setMoney(rewardInSlot.getMoney() + reward.getMoney());
											ItemMeta im = is.getItemMeta();
											im.setLore(rewardInSlot.getHiddenLore());
											String displayName = rewardInSlot.isItemReward()
													? plugin.getRewardManager().format(rewardInSlot.getMoney())
													: rewardInSlot.getDisplayName() + " ("
															+ plugin.getRewardManager().format(rewardInSlot.getMoney())
															+ ")";
											im.setDisplayName(ChatColor
													.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
													+ displayName);
											is.setItemMeta(im);
											is.setAmount(1);
											plugin.getMessages().debug(
													"Added %s to %s's item in slot %s, new value is %s",
													plugin.getRewardManager().format(reward.getMoney()),
													player.getName(), slot,
													plugin.getRewardManager().format(rewardInSlot.getMoney()));
											addedMoney = reward.getMoney();
											break;
										} else if ((reward.isKilledHeadReward() || reward.isKillerHeadReward())
												&& reward.getRewardType().equals(rewardInSlot.getRewardType())
												&& reward.getSkinUUID().equals(rewardInSlot.getSkinUUID())
												&& Misc.round(reward.getMoney()) == Misc
														.round(rewardInSlot.getMoney())) {
											ItemStack isPickup = item.getItemStack();
											if (is.getAmount() + isPickup.getAmount() <= 64) {
												is.setAmount(is.getAmount() + isPickup.getAmount());
												isPickup.setAmount(0);
												isPickup.setType(Material.AIR);

											} else {
												is.setAmount(64);
												isPickup.setAmount(is.getAmount() + isPickup.getAmount() - 64);
												plugin.getMessages().debug("%s merged two rewards(4)",
														player.getName());
											}
											plugin.getMessages().debug(
													"Added %s to %s's item in slot %s, new value is %s",
													plugin.getRewardManager().format(reward.getMoney()),
													player.getName(), slot,
													plugin.getRewardManager().format(rewardInSlot.getMoney()));
										}
									}
								}
							}
							if (Misc.round(addedMoney) == Misc.round(reward.getMoney())) {
								plugin.getMessages().debug("Was able to pickup all the money");
								item.remove();
								if (plugin.getRewardManager().getDroppedMoney().containsKey(entity.getEntityId()))
									plugin.getRewardManager().getDroppedMoney().remove(entity.getEntityId());
								if (ProtocolLibCompat.isSupported())
									ProtocolLibHelper.pickupMoney(player, item);

								if (reward.getMoney() == 0) {
									plugin.getMessages().debug("%s picked up a %s (# of rewards left=%s)",
											player.getName(), reward.getDisplayName(),
											plugin.getRewardManager().getDroppedMoney().size());
								} else {
									plugin.getMessages().debug(
											"%s picked up a %s with a value:%s (# of rewards left=%s)",
											player.getName(), reward.getDisplayName(),
											plugin.getRewardManager().format(Misc.round(reward.getMoney())),
											plugin.getRewardManager().getDroppedMoney().size());
									plugin.getMessages().playerActionBarMessageQueue(player,
											plugin.getMessages().getString("mobhunting.moneypickup", "money",
													plugin.getRewardManager().format(reward.getMoney()), "rewardname",
													ChatColor.valueOf(
															plugin.getConfigManager().dropMoneyOnGroundTextColor)
															+ (reward.isItemReward() ? plugin
																	.getConfigManager().dropMoneyOnGroundSkullRewardName
																			.trim()
																	: reward.getDisplayName())));
								}
							} else if (Misc.round(addedMoney) < Misc.round(reward.getMoney())) {
								double rest = reward.getMoney() - addedMoney;
								plugin.getMessages().debug("Was not able to pick up %s money (remove Item)", rest);
								item.remove();

								if (plugin.getRewardManager().getDroppedMoney().containsKey(entity.getEntityId()))
									plugin.getRewardManager().getDroppedMoney().remove(entity.getEntityId());
								if (ProtocolLibCompat.isSupported())
									ProtocolLibHelper.pickupMoney(player, item);

								plugin.getRewardManager().dropMoneyOnGround_RewardManager(player, null,
										player.getLocation(), rest);

							} else {
								plugin.getMessages().debug("someting else?????");
							}
						}
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onProjectileHitRewardEvent(ProjectileHitEvent event) {
		Projectile projectile = event.getEntity();
		Entity targetEntity = null;
		Iterator<Entity> nearby = projectile.getNearbyEntities(1, 1, 1).iterator();
		while (nearby.hasNext()) {
			targetEntity = nearby.next();

			if (Reward.isReward(targetEntity)) {
				if (plugin.getRewardManager().getDroppedMoney().containsKey(targetEntity.getEntityId()))
					plugin.getRewardManager().getDroppedMoney().remove(targetEntity.getEntityId());
				targetEntity.remove();
				plugin.getMessages().debug("The reward was hit by %s and removed. (# of rewards left=%s)",
						projectile.getType(), plugin.getRewardManager().getDroppedMoney().size());
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onRewardBlockPlace(BlockPlaceEvent event) {
		if (event.isCancelled())
			return;

		Player player = event.getPlayer();
		ItemStack is = event.getItemInHand();
		Block block = event.getBlockPlaced();

		if (isFakeReward(is)) {
			player.sendMessage(ChatColor.RED + "[MobHunting] WARNING, this was a FAKE reward with no value");
			return;
		}

		if (Reward.isReward(is)) {
			Reward reward = Reward.getReward(is);
			if (reward.checkHash()) {
				if (event.getPlayer().getGameMode() != GameMode.SURVIVAL) {
					reward.setMoney(0);
					plugin.getMessages().learn(event.getPlayer(),
							plugin.getMessages().getString("mobhunting.learn.no-duplication"));
				}
				reward.setUniqueId(UUID.randomUUID());
				plugin.getMessages().debug("%s placed a reward block: %s", player.getName(),
						ChatColor.stripColor(reward.toString()));
			} else {
				Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting]" + ChatColor.RED + "[Warning] "
						+ player.getName() + " has tried to change the value of a BagOfGold Item. Value set to 0!(6)");
				reward.setMoney(0);
			}
			block.setMetadata(Reward.MH_REWARD_DATA, new FixedMetadataValue(plugin, reward));
			plugin.getRewardManager().getReward().put(reward.getUniqueUUID(), reward);
			plugin.getRewardManager().getLocations().put(reward.getUniqueUUID(), block.getLocation());
			plugin.getRewardManager().saveReward(reward.getUniqueUUID());
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryCloseEvent(InventoryCloseEvent event) {
		Player player = (Player) event.getPlayer();
		Inventory inventory = event.getInventory();
		if (inventory.getType() == InventoryType.CRAFTING) {
			ItemStack helmet = player.getEquipment().getHelmet();

			if (isFakeReward(helmet)) {
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.RED + "[MobHunting] WARNING, you have a reward on your head. It was removed.");
				event.getPlayer().getEquipment().setHelmet(new ItemStack(Material.AIR));
				return;
			}

			if (Reward.isReward(helmet)) {
				Reward reward = Reward.getReward(helmet);
				if (reward.isBagOfGoldReward()) {
					plugin.getMessages().learn(player,
							plugin.getMessages().getString("mobhunting.learn.rewards.no-helmet"));
					event.getPlayer().getEquipment().setHelmet(new ItemStack(Material.AIR));
					if (Misc.round(reward.getMoney()) != Misc
							.round(plugin.getRewardManager().addBagOfGoldPlayer(player, reward.getMoney())))
						plugin.getRewardManager().dropMoneyOnGround_RewardManager(player, null, player.getLocation(),
								reward.getMoney());
				} else { // MobHead reward
					event.getPlayer().getEquipment().setHelmet(new ItemStack(Material.AIR));
					player.getWorld().dropItem(player.getLocation(), helmet);
				}

			}
		}
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
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ reward.getDisplayName());
			else
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ (plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("ITEM")
										? plugin.getRewardManager().format(reward.getMoney())
										: reward.getDisplayName() + " ("
												+ plugin.getRewardManager().format(reward.getMoney()) + ")"));
		} else if (Servers.isMC113OrNewer()
				&& (block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD)) {
			Skull skullState = (Skull) block.getState();
			OfflinePlayer owner = skullState.getOwningPlayer();
			if (owner != null && owner.getName() != null)
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor) + owner.getName());
		} else if (Materials.isSkull(block.getType())) {
			Skull skullState = (Skull) block.getState();
			switch (skullState.getSkullType()) {
			case PLAYER:
				if (Servers.isMC19OrNewer()) {
					OfflinePlayer owner = skullState.getOwningPlayer();
					if (owner != null && owner.getName() != null) {
						plugin.getMessages().playerActionBarMessageQueue(player,
								ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
										+ owner.getName());
					} else
						plugin.getMessages().playerActionBarMessageQueue(player,
								ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
										+ plugin.getMessages().getString("mobhunting.reward.customtexture"));
				} else if (skullState.hasOwner()) {
					@SuppressWarnings("deprecation")
					String owner = skullState.getOwner();
					if (owner != null && !owner.equalsIgnoreCase("")) {
						plugin.getMessages().playerActionBarMessageQueue(player,
								ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor) + owner);
					} else
						plugin.getMessages().playerActionBarMessageQueue(player,
								ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
										+ plugin.getMessages().getString("mobhunting.reward.customtexture"));
				} else
					plugin.getMessages().playerActionBarMessageQueue(player,
							ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
									+ plugin.getMessages().getString("mobhunting.reward.steve"));
				break;
			case CREEPER:
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ plugin.getMessages().getString("mobs.Creeper.name"));
				break;
			case SKELETON:
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ plugin.getMessages().getString("mobs.Skeleton.name"));
				break;
			case WITHER:
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ plugin.getMessages().getString("mobs.Wither.name"));
				break;
			case ZOMBIE:
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ plugin.getMessages().getString("mobs.Zombie.name"));
				break;
			case DRAGON:
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ plugin.getMessages().getString("mobs.EnderDragon.name"));
				break;
			default:
				break;
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryClickReward(InventoryClickEvent event) {
		if (event.isCancelled() || event.getInventory() == null) {
			plugin.getMessages().debug("RewardListeners: Something cancelled the InventoryClickEvent");
			return;
		}

		if (CitizensCompat.isNPC(event.getWhoClicked()))
			return;

		Player player = (Player) event.getWhoClicked();

		ItemStack isCurrentSlot = event.getCurrentItem();
		ItemStack isCursor = event.getCursor();
		ItemStack isKey = event.getHotbarButton() != -1 ? player.getInventory().getItem(event.getHotbarButton()) : null;

		if (isFakeReward(isCurrentSlot)) {
			isCurrentSlot.setType(Material.AIR);
			return;
		}
		if (isFakeReward(isCursor)) {
			isCursor.setType(Material.AIR);
			return;
		}
		if (isFakeReward(isKey)) {
			isKey.setType(Material.AIR);
			isKey.setAmount(0);
			return;
		}

		InventoryAction action = event.getAction();
		SlotType slotType = event.getSlotType();

		Inventory inventory = event.getInventory();
		if (action == InventoryAction.NOTHING)
			return;

		Inventory clickedInventory;
		if (Servers.isMC113OrNewer())
			clickedInventory = event.getClickedInventory();
		else
			clickedInventory = inventory;

		if (Reward.isReward(isCurrentSlot) || Reward.isReward(isCursor)) {

			plugin.getMessages().debug("action=%s, InvType=%s, slottype=%s, slotno=%s, current=%s, cursor=%s, view=%s",
					action, inventory.getType(), slotType, event.getSlot(),
					isCurrentSlot == null ? "null" : isCurrentSlot.getType(),
					isCursor == null ? "null" : isCursor.getType(), event.getView().getType());

			if (slotType == SlotType.OUTSIDE && Reward.isReward(isCursor)) {
				Reward reward = Reward.getReward(isCursor);
				plugin.getMessages().debug("RewardListerner: %s dropped %s BagOfGold outside the inventory",
						player.getName(), reward.getMoney());
				return;
			}

			List<InventoryType> allowedInventories;
			if (Servers.isMC114OrNewer())
				allowedInventories = Arrays.asList(InventoryType.PLAYER, InventoryType.BARREL, InventoryType.ANVIL,
						InventoryType.CHEST, InventoryType.DISPENSER, InventoryType.DROPPER, InventoryType.ENDER_CHEST,
						InventoryType.HOPPER, InventoryType.SHULKER_BOX, InventoryType.CRAFTING);
			else if (Servers.isMC19OrNewer())
				allowedInventories = Arrays.asList(InventoryType.PLAYER, InventoryType.ANVIL, InventoryType.CHEST,
						InventoryType.DISPENSER, InventoryType.DROPPER, InventoryType.ENDER_CHEST, InventoryType.HOPPER,
						InventoryType.SHULKER_BOX, InventoryType.CRAFTING);
			else // MC 1.8
				allowedInventories = Arrays.asList(InventoryType.PLAYER, InventoryType.ANVIL, InventoryType.CHEST,
						InventoryType.DISPENSER, InventoryType.DROPPER, InventoryType.ENDER_CHEST, InventoryType.HOPPER,
						InventoryType.CRAFTING);
			List<SlotType> allowedSlots = Arrays.asList(SlotType.CONTAINER, SlotType.QUICKBAR, SlotType.OUTSIDE);

			if (allowedSlots.contains(slotType)) {
				if (allowedInventories.contains(clickedInventory.getType())) {

					switch (action) {
					case UNKNOWN:
					case CLONE_STACK:
						if (Reward.isReward(isCurrentSlot) || Reward.isReward(isCursor)) {
							Reward reward = Reward.isReward(isCurrentSlot) ? Reward.getReward(isCurrentSlot)
									: Reward.getReward(isCursor);
							plugin.getMessages().learn(player, plugin.getMessages().getString(
									"mobhunting.learn.rewards.no-clone", "rewardname", reward.getDisplayName()));
							plugin.getMessages().debug("RewardListerner: %s its not allowed to clone BagOfGold",
									player.getName());
							event.setCancelled(true);
							return;
						}
						break;
					case COLLECT_TO_CURSOR:
						if (Reward.isReward(isCursor)) {
							Reward cursor = Reward.getReward(isCursor);
							if (cursor.isMoney()) {
								double saldo = Misc.floor(cursor.getMoney());
								for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
									ItemStack is = player.getInventory().getItem(slot);
									if (Reward.isReward(is)) {
										Reward reward = Reward.getReward(is);
										if ((reward.isBagOfGoldReward() || reward.isItemReward())
												&& reward.getMoney() > 0) {
											saldo = saldo + reward.getMoney();
											if (saldo <= plugin.getConfigManager().limitPerBag)
												player.getInventory().clear(slot);
											else {
												reward.setMoney(plugin.getConfigManager().limitPerBag);
												is = Reward.setDisplayNameAndHiddenLores(is.clone(), reward);
												is.setAmount(1);
												// event.setCurrentItem(is);
												player.getInventory().clear(slot);
												player.getInventory().addItem(is);
												saldo = saldo - plugin.getConfigManager().limitPerBag;
											}
										}
									}
								}
								cursor.setMoney(saldo);
								isCursor = Reward.setDisplayNameAndHiddenLores(isCursor.clone(), cursor);
								event.setCursor(isCursor);
								plugin.getMessages().debug("%s collected %s to the cursor", player.getName(), saldo);
							} else if (cursor.isKilledHeadReward() || cursor.isKillerHeadReward()) {
								plugin.getMessages()
										.debug("Collect to cursor on MobHunting heads is still not implemented");
								// plugin.getMessages().debug("%s collected %s to the cursor", player.getName(),
								// saldo);
							}
						}

						break;
					case DROP_ALL_CURSOR:
					case DROP_ALL_SLOT:
					case DROP_ONE_CURSOR:
					case DROP_ONE_SLOT:
						if (Reward.isReward(isCurrentSlot) || Reward.isReward(isCursor)) {
							plugin.getMessages().debug("%s dropped BagOfGold from inventory.", player.getName());
						}
						break;
					case HOTBAR_MOVE_AND_READD:
						if (action == InventoryAction.HOTBAR_MOVE_AND_READD) {
							if (Reward.isReward(isCurrentSlot) || Reward.isReward(isCursor)) {
								plugin.getMessages().debug("%s tried to do a HOTBAR_MOVE_AND_READD with a BagOfGold.",
										player.getName());
							}
						}
						break;
					case HOTBAR_SWAP:
						if (action == InventoryAction.HOTBAR_SWAP) {
							if (Reward.isReward(isCurrentSlot) || Reward.isReward(isCursor)) {
								plugin.getMessages().debug("%s tried to do a HATBAR_SWAP with a BagOfGold. Cancelled",
										player.getName());
								event.setCancelled(true);
								return;
							}
						}
						break;
					case MOVE_TO_OTHER_INVENTORY:
						if (allowedInventories.contains(inventory.getType())) {
							if (Reward.isReward(isCurrentSlot) || Reward.isReward(isCursor)) {
								plugin.getMessages().debug("%s tried to do a MOVE_TO_OTHER_INVENTORY with a BagOfGold.",
										player.getName());
							}
						} else {
							plugin.getMessages().debug("%s reward can't be moved into %s inventories.",
									player.getName(), inventory.getType());
						}
						break;
					case NOTHING:
						break;
					case PICKUP_HALF:
						if (isCursor.getType() == Material.AIR && Reward.isReward(isCurrentSlot)) {
							Reward reward = Reward.getReward(isCurrentSlot);
							if (reward.isMoney()) {
								double currentSlotMoney = Misc.round(reward.getMoney() / 2);
								double cursorMoney = Misc.round(reward.getMoney() - currentSlotMoney);
								if (cursorMoney >= plugin.getConfigManager().minimumReward) {
									event.setCancelled(true);
									reward.setMoney(currentSlotMoney);
									isCurrentSlot = Reward.setDisplayNameAndHiddenLores(isCurrentSlot.clone(), reward);
									event.setCurrentItem(isCurrentSlot);
									reward.setMoney(cursorMoney);
									isCursor = Reward.setDisplayNameAndHiddenLores(isCurrentSlot.clone(), reward);
									event.setCursor(isCursor);
									plugin.getMessages().debug("%s halfed a reward in two (%s,%s)", player.getName(),
											plugin.getRewardManager().format(currentSlotMoney),
											plugin.getRewardManager().format(cursorMoney));
								}
							}
							// else if (reward.isKilledHeadReward() || reward.isKilledHeadReward()) {
							// NOT NEEDED for heads
							// }
						}
						break;
					case PICKUP_ONE:
					case PICKUP_ALL:
					case PICKUP_SOME:
						if (Reward.isReward(isCurrentSlot)) {
							Reward reward = Reward.getReward(isCurrentSlot);
							plugin.getMessages().debug("%s moved %s (%s) out of Inventory", player.getName(),
									reward.getDisplayName(), reward.getMoney());
						}
						break;
					case PLACE_ALL:
						if (Reward.isReward(isCursor)) {
							Reward reward = Reward.getReward(isCursor);
							plugin.getMessages().debug("%s moved %s (%s) into Inventory", player.getName(),
									reward.getDisplayName(), reward.getMoney());
						}
						if (Reward.isReward(isCurrentSlot) && isCursor.getType() == Material.AIR) {
							Reward reward = Reward.getReward(isCurrentSlot);
							plugin.getMessages().debug("(2) %s moved %s (%s) out of Inventory", player.getName(),
									reward.getDisplayName(), reward.getMoney());
						}
						break;
					case PLACE_ONE:
					case PLACE_SOME:
					case SWAP_WITH_CURSOR:

						if (Reward.isReward(isCurrentSlot) && Reward.isReward(isCursor)) {
							ItemMeta imCurrent = isCurrentSlot.getItemMeta();
							ItemMeta imCursor = isCursor.getItemMeta();
							Reward reward1 = new Reward(imCurrent.getLore());
							Reward reward2 = new Reward(imCursor.getLore());
							if (reward1.isMoney() && reward1.getRewardType().equals(reward2.getRewardType())) {
								event.setCancelled(true);
								if (reward1.getMoney() + reward2.getMoney() <= plugin.getConfigManager().limitPerBag) {
									reward2.setMoney(reward1.getMoney() + reward2.getMoney());
									imCursor.setLore(reward2.getHiddenLore());
									imCursor.setDisplayName(ChatColor
											.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
											+ (plugin.getConfigManager().dropMoneyOnGroundItemtype
													.equalsIgnoreCase("ITEM")
															? plugin.getRewardManager().format(reward2.getMoney())
															: reward2.getDisplayName() + " (" + plugin
																	.getRewardManager().format(reward2.getMoney())
																	+ ")"));
									isCursor.setItemMeta(imCursor);
									isCurrentSlot.setAmount(0);
									isCurrentSlot.setType(Material.AIR);
									event.setCurrentItem(isCursor);
									event.setCursor(isCurrentSlot);
									plugin.getMessages().debug("%s merged two rewards(1)", player.getName());
								} else {
									double rest = reward1.getMoney() + reward2.getMoney()
											- plugin.getConfigManager().limitPerBag;
									reward2.setMoney(plugin.getConfigManager().limitPerBag);
									imCursor.setLore(reward2.getHiddenLore());
									imCursor.setDisplayName(ChatColor
											.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
											+ (plugin.getConfigManager().dropMoneyOnGroundItemtype
													.equalsIgnoreCase("ITEM")
															? plugin.getRewardManager()
																	.format(plugin.getConfigManager().limitPerBag)
															: reward2.getDisplayName() + " ("
																	+ plugin.getRewardManager().format(
																			plugin.getConfigManager().limitPerBag)
																	+ ")"));
									isCursor.setItemMeta(imCursor);

									reward1.setMoney(rest);
									imCurrent.setLore(reward1.getHiddenLore());
									imCurrent.setDisplayName(ChatColor
											.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
											+ (plugin.getConfigManager().dropMoneyOnGroundItemtype
													.equalsIgnoreCase("ITEM")
															? plugin.getRewardManager()
																	.format(plugin.getConfigManager().limitPerBag)
															: reward1.getDisplayName() + " (" + plugin
																	.getRewardManager().format(reward1.getMoney())
																	+ ")"));
									isCurrentSlot.setItemMeta(imCurrent);
									event.setCurrentItem(isCursor);
									event.setCursor(isCurrentSlot);
									plugin.getMessages().debug("%s merged two rewards(2)", player.getName());

								}

							} else if ((reward1.isKilledHeadReward() || reward1.isKillerHeadReward())
									&& reward1.getRewardType().equals(reward2.getRewardType())
									&& reward1.getSkinUUID().equals(reward2.getSkinUUID())
									&& Misc.round(reward1.getMoney()) == Misc.round(reward2.getMoney())) {
								event.setCancelled(true);
								if (isCursor.getAmount() + isCurrentSlot.getAmount() <= 64) {
									isCurrentSlot.setAmount(isCursor.getAmount() + isCurrentSlot.getAmount());
									isCursor.setAmount(0);
									isCursor.setType(Material.AIR);
									plugin.getMessages().debug("%s merged two rewards(3)", player.getName());
								} else {
									isCursor.setAmount(isCursor.getAmount() + isCurrentSlot.getAmount() - 64);
									isCurrentSlot.setAmount(64);
									plugin.getMessages().debug("%s merged two rewards(4)", player.getName());
								}
							}
						}
						break;
					}
				} else {
					plugin.getMessages().debug("%s its not allowed to use BagOfGold in a %s inventory",
							player.getName(), inventory.getType());
					event.setCancelled(true);
					return;
				}
			} else {
				plugin.getMessages().debug("%s its not allowed to use BagOfGold a %s slot", player.getName(), slotType);
				event.setCancelled(true);
				return;
			}
		} // No MobHunting Reward

	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockPhysicsEvent(BlockPhysicsEvent event) {
		if (event.isCancelled())
			return;

		if (event.getChangedType() != Material.matchMaterial("PLAYER_HEAD"))
			return;

		Block block = event.getBlock();

		if (Reward.isReward(block)) {
			Reward reward = Reward.getReward(block);

			// plugin.getMessages().debug("RewardListernes: Changed:%s, Src=%s, blk=%s" ,
			// event.getChangedType(), event.getSourceBlock().getType(),
			// event.getBlock().getType());

			if (event.getSourceBlock().getType() == Material.DISPENSER
					|| event.getSourceBlock().getType() == Material.matchMaterial("WATER")) {
				if (!Reward.isReward(event.getSourceBlock())) {
					// plugin.getMessages().debug("RewardListeners: a %s changed a %s(%s)",
					// event.getSourceBlock().getType(), block.getType(), reward.getMoney());
					plugin.getRewardManager().removeReward(block);
					plugin.getRewardManager().dropRewardOnGround(block.getLocation(), reward);
				}
			} else if (event.getSourceBlock().getType() == Material.matchMaterial("PLAYER_HEAD")) {
				// plugin.getMessages().debug("PLAYER_HEAD changed PLAYER_HEAD");
				return;
			} else {
				// plugin.getMessages().debug("RewardListeners: Event Cancelled - a %s tried to
				// change a %s(%s)",
				// event.getSourceBlock().getType(), block.getType(), reward.getMoney());
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockPistonExtendEvent(BlockPistonExtendEvent event) {
		if (event.isCancelled())
			return;
		@NotNull
		List<Block> changedBlocks = event.getBlocks();
		if (!changedBlocks.isEmpty())
			for (Block b : changedBlocks) {
				if (Reward.isReward(b)) {
					plugin.getMessages().debug("Is not possible to move a Reward with a Piston");
					event.setCancelled(true);
					return;
				}
			}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onRewardBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled())
			return;

		Block block = event.getBlock();
		if (Reward.isReward(block)) {
			Reward reward = Reward.getReward(block);
			plugin.getRewardManager().removeReward(block);
			plugin.getRewardManager().dropRewardOnGround(block.getLocation(), reward);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = (Player) event.getPlayer();
		if (player.getOpenInventory() != null) {
			if (player.getOpenInventory().getCursor() == null)
				return;
			if (!Reward.isReward(player.getOpenInventory().getCursor()))
				return;
			player.getOpenInventory().setCursor(null);
		}
	}
	
	
	/**
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryCreativeEvent(InventoryCreativeEvent event) {
		Player player = (Player) event.getWhoClicked();
		if (event.getSlotType()==SlotType.ARMOR) {
			if (Reward.isReward(event.getCursor())) {
				event.setCancelled(true);
				plugin.getMessages().debug("%s tried to place a BagOfGold in ARMOR slot",player.getName());
				plugin.getMessages().learn(player, plugin.getMessages().getString("mobhunting.learn.rewards.no-helmet"));
			}
		}
	}**/

}

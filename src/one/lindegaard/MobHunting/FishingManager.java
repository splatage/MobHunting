package one.lindegaard.MobHunting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import com.gmail.nossr50.datatypes.skills.PrimarySkillType;

import one.lindegaard.CustomItemsLib.Core;
import one.lindegaard.CustomItemsLib.Strings;
import one.lindegaard.CustomItemsLib.Tools;
import one.lindegaard.CustomItemsLib.messages.MessageType;
import one.lindegaard.CustomItemsLib.mobs.MobType;
import one.lindegaard.CustomItemsLib.rewards.CoreCustomItems;
import one.lindegaard.CustomItemsLib.rewards.Reward;
import one.lindegaard.CustomItemsLib.rewards.RewardType;
import one.lindegaard.MobHunting.compatibility.FactionsHelperCompat;
import one.lindegaard.MobHunting.compatibility.McMMOCompat;
import one.lindegaard.MobHunting.events.MobHuntFishingEvent;
import one.lindegaard.MobHunting.mobs.ExtendedMob;
import one.lindegaard.MobHunting.modifier.DifficultyBonus;
import one.lindegaard.MobHunting.modifier.FactionWarZoneBonus;
import one.lindegaard.MobHunting.modifier.HappyHourBonus;
import one.lindegaard.MobHunting.modifier.IModifier;
import one.lindegaard.MobHunting.modifier.RankBonus;
import one.lindegaard.MobHunting.modifier.WorldBonus;

public class FishingManager implements Listener {

	private MobHunting plugin;
	private Set<IModifier> mFishingModifiers = new HashSet<IModifier>();

	public FishingManager(MobHunting plugin) {
		this.plugin = plugin;
		if (plugin.getConfigManager().enableFishingRewards) {
			registerFishingModifiers();
			Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		}
	}

	private void registerFishingModifiers() {
		mFishingModifiers.add(new DifficultyBonus());
		mFishingModifiers.add(new WorldBonus());
		mFishingModifiers.add(new HappyHourBonus());
		mFishingModifiers.add(new RankBonus());
		if (FactionsHelperCompat.isSupported())
			mFishingModifiers.add(new FactionWarZoneBonus());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void Fish(PlayerFishEvent event) {

		if (event.isCancelled()) {
			plugin.getMessages().debug("FishingEvent: event was cancelled");
			return;
		}

		Player player = event.getPlayer();
		if (player == null) {
			plugin.getMessages().debug("FishingEvent: player was null");
			return;
		}

		if (!plugin.getMobHuntingManager().isHuntEnabled(player)) {
			plugin.getMessages().debug("FishingEvent %s: Player doesnt have permission mobhunting.enable",
					player.getName());
			return;
		}

		State state = event.getState();
		Entity fish = event.getCaught();

		if (fish == null || (fish != null && !(fish instanceof Item)))
			plugin.getMessages().debug("FishingEvent: State=%s", state);
		else
			plugin.getMessages().debug("FishingEvent: State=%s, %s caught a %s", state, player.getName(),
					((Item) fish).getItemStack().getType());

		switch (state) {
		case CAUGHT_ENTITY:
			// When a player has successfully caught an entity
			plugin.getMessages().debug("FishingBlocked: %s caught a flowting item in the water, no reward",
					player.getName());
			break;
		case CAUGHT_FISH:
			// When a player has successfully caught a fish and is reeling it
			// in.
			// break;
			if (player.getGameMode() != GameMode.SURVIVAL) {
				plugin.getMessages().debug("FishingBlocked: %s is not in survival mode", player.getName());
				plugin.getMessages().learn(player, plugin.getMessages().getString("mobhunting.learn.survival"));
				return;
			}

			if (fish == null || !(fish instanceof Item) || (
			// Minecraft 1.13+
			((Item) fish).getItemStack().getType() != Material.matchMaterial("SALMON")
					&& ((Item) fish).getItemStack().getType() != Material.matchMaterial("COD")
					&& ((Item) fish).getItemStack().getType() != Material.matchMaterial("TROPICAL_FISH")

					// Minecraft 1.8+
					&& ((Item) fish).getItemStack().getType() != Material.matchMaterial("PUFFERFISH")

					// Minecraft 1.8-1.12.2
					&& ((Item) fish).getItemStack().getType() != Material.matchMaterial("RAW_SALMON")
					&& ((Item) fish).getItemStack().getType() != Material.matchMaterial("RAW_FISH")
					&& ((Item) fish).getItemStack().getType() != Material.matchMaterial("CLOWNFISH"))) {
				plugin.getMessages().debug("FishingBlocked: %s only get rewards for fish", player.getName());

				return;
			}

			Material material_under_hook = fish.getLocation().getBlock().getType();
			if (!(material_under_hook == Material.WATER)) {
				plugin.getMessages().debug("FishingBlocked: %s was fishing on %s", player.getName(),
						material_under_hook);
				return;
			}

			plugin.getMessages().debug("fish id=%s", fish.getEntityId());

			if (fish.hasMetadata("MH:FishCaught")) {
				plugin.getMessages().learn(player,
						plugin.getMessages().getString("mobhunting.fishcaught.the_same_fish"));
				plugin.getMessages().debug("FishingBlocked %s: Player caught the same fish again", player.getName());
				return;
			}

			// Calculate basic the reward
			ExtendedMob extendedMob = plugin.getExtendedMobManager().getExtendedMobFromEntity(fish);
			if (extendedMob.getMob_id() == 0) {
				Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX_WARNING + "Unknown Mob:"
						+ extendedMob.getMobName() + " from plugin " + extendedMob.getMobPlugin());
				Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX_WARNING + "Please report this to developer!");
				return;
			}
			double cash = plugin.getRewardManager().getBaseKillPrize(fish);

			plugin.getMessages().debug("Basic Prize=%s for catching a %s", plugin.getEconomyManager().format(cash),
					extendedMob.getMobName());

			// Apply the modifiers to Basic reward
			double multipliers = 1.0;
			HashMap<String, Double> multiplierList = new HashMap<String, Double>();
			ArrayList<String> modifiers = new ArrayList<String>();
			for (IModifier mod : mFishingModifiers) {
				if (mod.doesApply(fish, player, null, null, null)) {
					double amt = mod.getMultiplier(fish, player, null, null, null);
					if (amt != 1.0) {
						plugin.getMessages().debug("Multiplier: %s = %s", mod.getName(), amt);
						modifiers.add(mod.getName());
						multiplierList.put(mod.getName(), amt);
						multipliers *= amt;
					}
				}
			}

			String extraString = "";

			// Only display the multiplier if its not 1
			if (Math.abs(multipliers - 1) > 0.05)
				extraString += String.format("x%.1f", multipliers);

			// Add on modifiers
			int i = 0;
			for (String modifier : modifiers) {
				if (i == 0)
					extraString += ChatColor.WHITE + " ( " + modifier;
				else
					extraString += ChatColor.WHITE + " * " + modifier;
				i++;
			}
			if (i != 0)
				extraString += ChatColor.WHITE + " ) ";

			cash *= multipliers;

			cash = Tools.ceil(cash);

			// Pay the reward to player and assister
			if (cash >= Core.getConfigManager().minimumReward || cash <= -Core.getConfigManager().minimumReward
					|| !plugin.getRewardManager().getKillCommands(fish).isEmpty()) {

				// Handle MobHuntFishingEvent
				MobHuntFishingEvent event2 = new MobHuntFishingEvent(player, fish, cash, multiplierList);
				Bukkit.getPluginManager().callEvent(event2);
				if (event2.isCancelled()) {
					plugin.getMessages().debug("FishingBlocked %s: MobHuntFishingEvent was cancelled by another plugin",
							player.getName());
					return;
				}

				fish.setMetadata("MH:FishCaught", new FixedMetadataValue(plugin, true));

				if (cash >= Core.getConfigManager().minimumReward) {
					plugin.getEconomyManager().depositPlayer(player, cash);
					plugin.getMessages().debug("%s got a reward (%s)", player.getName(),
							plugin.getEconomyManager().format(cash));
				} else if (cash <= -Core.getConfigManager().minimumReward) {
					plugin.getEconomyManager().withdrawPlayer(player, -cash);
					plugin.getMessages().debug("%s got a penalty (%s)", player.getName(),
							plugin.getEconomyManager().format(cash));
				}

				// Record Fishing Achievement is done using
				// SeventhHuntAchievement.java (onFishingCompleted)

				String fishermanPos = player.getLocation().getBlockX() + " " + player.getLocation().getBlockY() + " "
						+ player.getLocation().getBlockZ();
				String killedpos = fish.getLocation().getBlockX() + " " + fish.getLocation().getBlockY() + " "
						+ fish.getLocation().getBlockZ();

				// Record the kill in the Database
				if (player != null) {
					plugin.getMessages().debug("RecordFishing: %s caught a %s (%s)", player.getName(),
							extendedMob.getMobName(), extendedMob.getMobPlugin().name());
					plugin.getDataStoreManager().recordKill(player, extendedMob, player.hasMetadata("MH:hasBonus"),
							cash);
				}

				// Handle Muted mode
				boolean fisherman_muted = false;
				if (Core.getPlayerSettingsManager().containsKey(player))
					fisherman_muted = Core.getPlayerSettingsManager().getPlayerSettings(player).isMuted();

				// Tell the player that he got the reward/penalty,
				// unless
				// muted
				if (!fisherman_muted)
					if (extraString.trim().isEmpty()) {
						if (cash >= Core.getConfigManager().minimumReward) {
							plugin.getMessages().playerActionBarMessageQueue(player,
									ChatColor.GREEN + "" + ChatColor.ITALIC
											+ plugin.getMessages().getString("mobhunting.fishcaught.reward", "prize",
													plugin.getEconomyManager().format(cash)));
						} else if (cash <= -Core.getConfigManager().minimumReward) {
							plugin.getMessages().playerActionBarMessageQueue(player,
									ChatColor.RED + "" + ChatColor.ITALIC
											+ plugin.getMessages().getString("mobhunting.fishcaught.penalty", "prize",
													plugin.getEconomyManager().format(cash)));
						}

					} else {
						if (cash >= Core.getConfigManager().minimumReward) {
							plugin.getMessages().debug("Message to send to ActionBar=%s", ChatColor.GREEN + ""
									+ ChatColor.ITALIC
									+ plugin.getMessages().getString("mobhunting.fishcaught.reward.bonuses", "prize",
											plugin.getEconomyManager().format(cash), "bonuses", extraString.trim(),
											"multipliers", plugin.getEconomyManager().format(multipliers)));
							plugin.getMessages().playerActionBarMessageQueue(player, ChatColor.GREEN + ""
									+ ChatColor.ITALIC
									+ plugin.getMessages().getString("mobhunting.fishcaught.reward.bonuses", "prize",
											plugin.getEconomyManager().format(cash), "bonuses", extraString.trim(),
											"multipliers", plugin.getEconomyManager().format(multipliers)));
						} else if (cash <= -Core.getConfigManager().minimumReward) {
							plugin.getMessages().playerActionBarMessageQueue(player, ChatColor.RED + ""
									+ ChatColor.ITALIC
									+ plugin.getMessages().getString("mobhunting.fishcaught.penalty.bonuses", "prize",
											plugin.getEconomyManager().format(cash), "bonuses", extraString.trim(),
											"multipliers", plugin.getEconomyManager().format(multipliers)));
						} else
							plugin.getMessages().debug("FishingBlocked %s: Reward was less than %s", player.getName(),
									Core.getConfigManager().minimumReward);
					}

				// McMMO Experience rewards
				if (McMMOCompat.isSupported() && plugin.getConfigManager().enableMcMMOLevelRewards) {
					double chance = plugin.mRand.nextDouble();
					int level = plugin.getRewardManager().getMcMMOLevel(fish);
					plugin.getMessages().debug("If %s<%s %s will get a McMMO Level for fishing", chance,
							plugin.getRewardManager().getMcMMOChance(fish), player.getName());
					if (chance < plugin.getRewardManager().getMcMMOChance(fish)) {
						McMMOCompat.addLevel(player, PrimarySkillType.FISHING.getName(), level);
						plugin.getMessages().debug("%s was rewarded with %s McMMO level for Fishing", player.getName(),
								level);
						plugin.getMessages().playerSendMessage(player,
								plugin.getMessages().getString("mobhunting.mcmmo.fishing_level", "mcmmo_level", level));
					}
				}

				List<HashMap<String, String>> fishCommands = new ArrayList<HashMap<String, String>>();
				fishCommands = plugin.getRewardManager().getKillCommands(fish);

				Iterator<HashMap<String, String>> itr = fishCommands.iterator();
				while (itr.hasNext()) {
					HashMap<String, String> cmd = itr.next();
					String perm = cmd.getOrDefault("permission", "");
					if (perm.isEmpty() || player.hasPermission(perm)) {
						double randomNumber = plugin.mRand.nextDouble();
						double chance = 0;
						try {
							chance = Double.valueOf(cmd.get("chance"));
						} catch (Exception e) {
							Bukkit.getConsoleSender()
									.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RED
											+ " The chance to run a command when catching a " + fish.getName()
											+ " must be formatted as a string ex. chance: '0.5'");
						}
						if (randomNumber < chance) {
							String worldname = player.getWorld().getName();
							String prizeCommand = cmd.get("cmd").replaceAll("\\{player\\}", player.getName())
									.replaceAll("\\{killer\\}", player.getName()).replaceAll("\\{world\\}", worldname)
									.replaceAll("\\{prize\\}", plugin.getEconomyManager().format(cash))
									.replaceAll("\\{killerpos\\}", fishermanPos)
									.replaceAll("\\{rewardname\\}", Core.getConfigManager().bagOfGoldName.trim());
							plugin.getMessages().debug("command to be run is:" + prizeCommand);
							if (!plugin.getRewardManager().getKillCommands(fish).isEmpty()) {
								String str = prizeCommand;
								do {
									if (str.contains("|")) {
										int n = str.indexOf("|");
										Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
												str.substring(0, n));
										str = str.substring(n + 1, str.length()).toString();
									}
								} while (str.contains("|"));
								Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), str);
							}

							// send the command message to the player
							MessageType messageType = MessageType
									.valueOf((cmd == null || cmd.get("message_type") == null) ? "Chat"
											: cmd.getOrDefault("message_type", "Chat"));
							String message = plugin.getRewardManager().getKillMessage(fish)
									.replaceAll("\\{player\\}", player.getName())
									.replaceAll("\\{killer\\}", player.getName())
									.replaceAll("\\{killed\\}", extendedMob.getFriendlyName())
									.replaceAll("\\{world\\}", worldname)
									.replaceAll("\\{prize\\}", plugin.getEconomyManager().format(cash))
									.replaceAll("\\{world\\}", player.getWorld().getName())
									.replaceAll("\\{killerpos\\}", fishermanPos)
									.replaceAll("\\{rewardname\\}", Core.getConfigManager().bagOfGoldName.trim());
							if (!message.isEmpty()) {
								plugin.getMessages().playerSendMessageAt(player, message, messageType);
							}

						} else
							plugin.getMessages().debug(
									"The command did not run because random number (%s) was bigger than chance (%s)",
									randomNumber, cmd.get("chance"));
					} else {
						plugin.getMessages().debug("%s has not permission (%s) to run command: %s", player.getName(),
								cmd.get("permission"), cmd.get("cmd"));
					}

					String message = Strings.convertColors(plugin.getRewardManager().getKillMessage(fish));
					if (!message.isEmpty() && !fisherman_muted) {
						plugin.getMessages().playerSendMessage(player, ChatColor.GREEN + "" + ChatColor.ITALIC
								+ message.replaceAll("\\{player\\}", player.getName())
										.replaceAll("\\{killer\\}", player.getName())
										.replaceAll("\\{killed\\}", extendedMob.getFriendlyName())
										.replaceAll("\\{prize\\}", plugin.getEconomyManager().format(cash))
										.replaceAll("\\{world\\}", player.getWorld().getName())
										.replaceAll("\\{rewardname\\}", Core.getConfigManager().bagOfGoldName.trim()));
					}
				}

				// drop a head if allowed
				if (plugin.getRewardManager().getHeadDropHead(fish)) {
					double random = plugin.mRand.nextDouble();
					if (random < plugin.getRewardManager().getHeadDropChance(fish)) {
						MobType minecraftMob = MobType.getMobType(fish);
						ItemStack head = new CoreCustomItems(plugin).getCustomHead(minecraftMob,
								minecraftMob.getFriendlyName(), 1, plugin.getRewardManager().getHeadValue(fish),
								minecraftMob.getSkinUUID());
						head = Reward.setDisplayNameAndHiddenLores(head,
								new Reward(minecraftMob.getFriendlyName(), plugin.getRewardManager().getHeadValue(fish),
										RewardType.KILLED, minecraftMob.getSkinUUID()));
						fish.getWorld().dropItem(fish.getLocation(), head);
						plugin.getMessages().debug("%s caught a %s and a head was dropped in the water",
								player.getName(), fish.getName());
						if (!plugin.getRewardManager().getHeadDropMessage(fish).isEmpty())
							plugin.getMessages().playerSendMessage(player,
									ChatColor.GREEN + Strings.convertColors(plugin.getRewardManager()
											.getHeadDropMessage(fish).replaceAll("\\{player\\}", player.getName())
											.replaceAll("\\{killer\\}", player.getName())
											.replaceAll("\\{killed\\}", minecraftMob.getFriendlyName())
											.replaceAll("\\{prize\\}", plugin.getEconomyManager().format(cash))
											.replaceAll("\\{world\\}", player.getWorld().getName())
											.replaceAll("\\{killerpos\\}", fishermanPos)
											.replaceAll("\\{killedpos\\}", killedpos).replaceAll("\\{rewardname\\}",
													Core.getConfigManager().bagOfGoldName.trim())));
					} else {
						plugin.getMessages().debug("Did not drop a head: random(%s)>chance(%s)", random,
								plugin.getRewardManager().getHeadDropChance(fish));
					}
				}
			}
			break;
		case BITE:
			// Called when there is a bite on the hook and it is ready to be
			// reeled in.
			break;
		case FAILED_ATTEMPT:
			// When a player fails to catch anything while fishing usually due
			// to poor aiming or timing
			break;
		case FISHING:
			// When a player is fishing, ie casting the line out.
			break;
		case IN_GROUND:
			// When a bobber is stuck in the ground
			// plugin.getMessages().debug("State is IN_GROUND");
			break;
		// default:
		// break;
		case REEL_IN:
			break;
		default:
			break;

		}

	}

	public Set<IModifier> getFishingModifiers() {
		return mFishingModifiers;
	}

}

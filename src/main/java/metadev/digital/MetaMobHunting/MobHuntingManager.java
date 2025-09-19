package metadev.digital.MetaMobHunting;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.compatibility.addons.BattleArenaCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.CitizensCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.CrackShotCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.DisguisesHelper;
import metadev.digital.MetaMobHunting.compatibility.addons.EliteMobsCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.EssentialsCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.FactionsUUIDCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.ImprovedFactionsCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.McMMOCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.MobArenaCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.MyPetCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.MysteriousHalloweenCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.MythicMobsCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.PlaceholderAPICompat;
import metadev.digital.MetaMobHunting.compatibility.addons.PVPArenaCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.ResidenceCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.StackMobCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.TownyCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.WeaponMechanicsCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.WorldGuardCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.WorldGuardMobHuntingFlag;
import metadev.digital.metabagofgold.BagOfGold;
import metadev.digital.metabagofgold.PlayerBalance;
import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.Strings;
import metadev.digital.metacustomitemslib.Tools;
import metadev.digital.metacustomitemslib.materials.Materials;
import metadev.digital.metacustomitemslib.messages.MessageType;
import metadev.digital.metacustomitemslib.mobs.MobType;
import metadev.digital.metacustomitemslib.rewards.CoreCustomItems;
import metadev.digital.metacustomitemslib.server.Server;
import metadev.digital.MetaMobHunting.bounty.Bounty;
import metadev.digital.MetaMobHunting.bounty.BountyStatus;
import metadev.digital.MetaMobHunting.events.BountyKillEvent;
import metadev.digital.MetaMobHunting.events.MobHuntEnableCheckEvent;
import metadev.digital.MetaMobHunting.events.MobHuntKillEvent;
import metadev.digital.MetaMobHunting.grinding.Area;
import metadev.digital.MetaMobHunting.mobs.ExtendedMob;
import metadev.digital.MetaMobHunting.modifier.*;
import metadev.digital.MetaMobHunting.placeholder.PlaceHolderData;

import org.bukkit.*;
import org.bukkit.command.CommandException;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class MobHuntingManager implements Listener {

	private MobHunting plugin;
	private final String SPAWNER_BLOCKED = "MH:SpawnerBlocked";

	private static WeakHashMap<LivingEntity, DamageInformation> mDamageHistory = new WeakHashMap<>();
	private Set<IModifier> mHuntingModifiers = new HashSet<>();

	/**
	 * Constructor for MobHuntingManager
	 *
	 * @param instance
	 */
	public MobHuntingManager(MobHunting instance) {
		this.plugin = instance;
		registerHuntingModifiers();
		MessageHelper.debug("Register MobHunting Events");
		Bukkit.getServer().getPluginManager().registerEvents(this, instance);
	}

	/**
	 * Gets the DamageInformation for a LivingEntity
	 *
	 * @param entity
	 * @return
	 */
	public DamageInformation getDamageInformation(Entity entity) {
		return mDamageHistory.get(entity);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		setHuntEnabled(player, true);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	private void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		Player player = event.getPlayer();
		HuntData data = new HuntData(player);
		if (data.getKillstreakLevel() != 0 && data.getKillstreakMultiplier() != 1) {
			plugin.getMessages().playerActionBarMessageQueue(player, ChatColor.RED + "" + ChatColor.ITALIC
					+ plugin.getMessages().getString("mobhunting.killstreak.ended"));
		}
		data.setKillStreak(0);
		data.putHuntDataToPlayer(player);
	}

	/**
	 * Set if MobHunting is allowed for the player
	 *
	 * @param player
	 * @param enabled = true : means the MobHunting is allowed
	 */
	public void setHuntEnabled(Player player, boolean enabled) {
		player.setMetadata("MH:enabled", new FixedMetadataValue(plugin, enabled));
	}

	/**
	 * Checks if MobHunting is enabled for the player
	 *
	 * @param player
	 * @return true if MobHunting is enabled for the player, false if not.
	 */
	public boolean isHuntEnabled(Player player) {
		if (CitizensCompat.isNPC(player))
			return false;

		if (!player.hasMetadata("MH:enabled")) {
			MessageHelper.debug("KillBlocked %s: Player doesn't have MH:enabled", player.getName());
			return false;
		}

		List<MetadataValue> values = player.getMetadata("MH:enabled");

		// Use the first value that matches the required type
		boolean enabled = false;
		for (MetadataValue value : values) {
			if (value.value() instanceof Boolean)
				enabled = value.asBoolean();
		}

		if (enabled && !player.hasPermission("mobhunting.enable")) {
			MessageHelper.debug("KillBlocked %s: PlaMcMMO_Versionyer doesnt have permission mobhunting.enable",
					player.getName());
			return false;
		}

		if (!enabled) {
			MessageHelper.debug("KillBlocked %s: MH:enabled is false", player.getName());
			return false;
		}

		MobHuntEnableCheckEvent event = new MobHuntEnableCheckEvent(player);
		Bukkit.getPluginManager().callEvent(event);

		if (!event.isEnabled())
			MessageHelper.debug("KillBlocked %s: Plugin cancelled check", player.getName());
		return event.isEnabled();
	}

	private void registerHuntingModifiers() {
		mHuntingModifiers.add(new BonusMobBonus());
		mHuntingModifiers.add(new BrawlerBonus());
		if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.LevelledMobs.getName())))
			mHuntingModifiers.add(new LevelledMobsBonus());
		mHuntingModifiers.add(new CoverBlown());
		mHuntingModifiers.add(new CriticalModifier());
		mHuntingModifiers.add(new DifficultyBonus());
		mHuntingModifiers.add(new WorldBonus());
		if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Factions.getName())))
			mHuntingModifiers.add(new FactionWarZoneBonus());
		mHuntingModifiers.add(new FlyingPenalty());
		mHuntingModifiers.add(new FriendleFireBonus());
		if (plugin.getConfigManager().areaGrindingDetectionEnabled
				&& plugin.getConfigManager().grindingDetectionEnabled)
			mHuntingModifiers.add(new AreaGrindingPenalty());
		mHuntingModifiers.add(new HappyHourBonus());
		mHuntingModifiers.add(new MountedBonus());
		mHuntingModifiers.add(new ProSniperBonus());
		mHuntingModifiers.add(new RankBonus());
		mHuntingModifiers.add(new ReturnToSenderBonus());
		mHuntingModifiers.add(new ShoveBonus());
		mHuntingModifiers.add(new SneakyBonus());
		mHuntingModifiers.add(new SniperBonus());
		if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.StackMob.getName())))
			mHuntingModifiers.add(new StackedMobBonus());
		mHuntingModifiers.add(new Undercover());
		if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.CrackShot.getName())))
			mHuntingModifiers.add(new CrackShotPenalty());
		if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.WeaponMechanics.getName())))
			mHuntingModifiers.add(new WeaponMechanicsPenalty());
	}

	/**
	 * Check if MobHunting is allowed in world
	 *
	 * @param world
	 * @return true if MobHunting is allowed.
	 */
	public boolean isHuntEnabledInWorld(World world) {
		if (world != null)
			for (String worldName : plugin.getConfigManager().disabledInWorlds) {
				if (world.getName().equalsIgnoreCase(worldName))
					return false;
			}

		return true;
	}

	/**
	 * Checks if the player has permission to kill the mob
	 *
	 * @param player
	 * @param mob
	 * @return true if the player has permission to kill the mob
	 */
	public boolean hasPermissionToKillMob(Player player, LivingEntity mob) {
		String permission_postfix = "*";

		if (MythicMobsCompat.isMythicMob(mob)) {
			permission_postfix = MythicMobsCompat.getMythicMobType(mob);
			if (player.isPermissionSet("mobhunting.mobs." + permission_postfix))
				return player.hasPermission("mobhunting.mobs." + permission_postfix);
			else {
				MessageHelper.debug("Permission mobhunting.mobs." + permission_postfix + " not set, defaulting to True.");
				return true;
			}
		} else if (CitizensCompat.isSentryOrSentinelOrSentries(mob)) {
			permission_postfix = "npc-" + CitizensCompat.getNPCId(mob);
			if (player.isPermissionSet("mobhunting.mobs." + permission_postfix))
				return player.hasPermission("mobhunting.mobs." + permission_postfix);
			else {
				MessageHelper.debug("Permission mobhunting.mobs.'" + permission_postfix + "' not set, defaulting to True.");
				return true;
			}
		} else if (MysteriousHalloweenCompat.isMysteriousHalloween(mob)) {
            permission_postfix = "npc-" + MysteriousHalloweenCompat.getMysteriousHalloweenType(mob);
            if (player.isPermissionSet("mobhunting.mobs." + permission_postfix))
                return player.hasPermission("mobhunting.mobs." + permission_postfix);
            else {
                MessageHelper.debug("Permission mobhunting.mobs.'" + permission_postfix + "' not set, defaulting to True.");
                return true;
            }
        } else {
			permission_postfix = mob.getType().toString();
			if (player.isPermissionSet("mobhunting.mobs." + permission_postfix))
				return player.hasPermission("mobhunting.mobs." + permission_postfix);
			else {
				MessageHelper.debug("Permission 'mobhunting.mobs.*' or 'mobhunting.mobs." + permission_postfix + "' not set, defaulting to True.");
				return true;
			}
		}
	}

	// ************************************************************************************
	// EVENTS
	// ************************************************************************************
	@EventHandler(priority = EventPriority.NORMAL)
	private void onPlayerDeath(PlayerDeathEvent event) {
		if (!isHuntEnabledInWorld(event.getEntity().getWorld()) || !isHuntEnabled(event.getEntity()))
			return;

		Player killed = event.getEntity();

		HuntData data = new HuntData(killed);
		if (data.getKillstreakLevel() != 0 && data.getKillstreakMultiplier() != 1) {
			plugin.getMessages().playerActionBarMessageQueue((Player) event.getEntity(), ChatColor.RED + ""
					+ ChatColor.ITALIC + plugin.getMessages().getString("mobhunting.killstreak.ended"));
		}
		MessageHelper.debug("%s died - Killstreak ended", killed.getName());
		data.resetKillStreak(killed);

		if (CitizensCompat.isNPC(killed))
			return;

		EntityDamageEvent lastDamageCause = killed.getLastDamageCause();
		if (lastDamageCause instanceof EntityDamageByEntityEvent) {
			Entity damager = ((EntityDamageByEntityEvent) lastDamageCause).getDamager();
			Player killer = null;
			LivingEntity mob = null;

			if (damager instanceof Player)
				killer = (Player) damager;
			else if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player)
				killer = (Player) ((Projectile) damager).getShooter();
			else if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof LivingEntity)
				mob = (LivingEntity) ((Projectile) damager).getShooter();
			else if (damager instanceof LivingEntity)
				mob = (LivingEntity) damager;
			else if (damager instanceof Projectile) {
				if (((Projectile) damager).getShooter() != null)
					MessageHelper.debug("%s was killed by a %s shot by %s", killed.getName(), damager.getName(),
							((Projectile) damager).getShooter().toString());
				else
					MessageHelper.debug("%s was killed by a %s", killed.getName(), damager.getName());
			}

			MessageHelper.debug("%s was killed by a %s", killed.getName(), damager.getName());
			if (damager instanceof Projectile)
				MessageHelper.debug("and shooter was %s", ((Projectile) damager).getShooter().toString());

			// PVPArena
			if (PVPArenaCompat.isPlayingPVPArena((Player) killed)
					&& !plugin.getConfigManager().pvparenaGetRewards) {
				MessageHelper.debug("KillBlocked: %s was killed while playing PvpArena.", killed.getName());
				return;
            // BattleArena
			} else if (BattleArenaCompat.isPlayingBattleArena((Player) killed)) {
                MessageHelper.debug("KillBlocked: %s was killed while playing BattleArena.", killed.getName());
                return;
            // MobArena
            } else if(MobArenaCompat.isPlayingMobArena((Player) killed) && !plugin.getConfigManager().mobarenaGetRewards) {
                MessageHelper.debug("KillBlocked: %s was killed while playing MobArena.", killed.getName());
                return;
            }

			if (mob != null) {
				double playerKilledByMobPenalty = 0;

				playerKilledByMobPenalty = plugin.getRewardManager().getPlayerKilledByMobPenalty(killed,
						event.getDrops());

				if (playerKilledByMobPenalty != 0) {
					plugin.getEconomyManager().withdrawPlayer(killed, playerKilledByMobPenalty);
					boolean killed_muted = false;
					if (Core.getPlayerSettingsManager().containsKey(killed))
						killed_muted = Core.getPlayerSettingsManager().getPlayerSettings(killed).isMuted();
					if (!killed_muted) {
						plugin.getMessages().playerActionBarMessageQueue(killed,
								ChatColor.RED + "" + ChatColor.ITALIC
										+ plugin.getMessages().getString("mobhunting.moneylost", "prize",
												plugin.getEconomyManager().format(playerKilledByMobPenalty), "money",
												plugin.getEconomyManager().format(playerKilledByMobPenalty)));
					}
					MessageHelper.debug("%s lost %s for being killed by a %s", killed.getName(),
							plugin.getEconomyManager().format(playerKilledByMobPenalty), mob.getName());
				} else {
					MessageHelper.debug("There is NO penalty for being killed by a %s", mob.getName());
				}

			} else

			if (killer != null && MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.BagOfGold.getName()))) {
				PlayerBalance ps = BagOfGold.getInstance().getPlayerBalanceManager().getPlayerBalance(killed);
				double balance = ps.getBalance() + ps.getBalanceChanges();
				if (balance != 0) {
					MessageHelper.debug("%s dropped %s because of his death, killed by %s", killed.getName(),
							plugin.getEconomyManager().format(balance), killer.getName());
					BagOfGold.getAPI().removeMoneyFromPlayerBalance(killed, balance);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlayerDamage(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;

		if (!isHuntEnabledInWorld(event.getEntity().getWorld()) || !isHuntEnabled((Player) event.getEntity()))
			return;

		Player player = (Player) event.getEntity();
		HuntData data = new HuntData(player);
		if (data.getKillstreakLevel() != 0 && data.getKillstreakMultiplier() != 1) {
			plugin.getMessages().playerActionBarMessageQueue(player, ChatColor.RED + "" + ChatColor.ITALIC
					+ plugin.getMessages().getString("mobhunting.killstreak.ended"));
			MessageHelper.debug("%s was hit - Killstreak ended", player.getName());
		}
		data.resetKillStreak(player);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onSkeletonShoot(ProjectileLaunchEvent event) {
		if (!isHuntEnabledInWorld(event.getEntity().getWorld()))
			return;

		if (event.getEntity() instanceof Arrow) {
			if (event.getEntity().getShooter() instanceof Skeleton) {
				Skeleton shooter = (Skeleton) event.getEntity().getShooter();
				if (shooter.getTarget() instanceof Player && isHuntEnabled((Player) shooter.getTarget())
						&& ((Player) shooter.getTarget()).getGameMode() != GameMode.CREATIVE) {
					DamageInformation info = null;
					info = mDamageHistory.get(shooter);
					if (info == null)
						info = new DamageInformation();
					info.setTime(System.currentTimeMillis());
					info.setAttacker((Player) shooter.getTarget());
					info.setAttackerPosition(shooter.getTarget().getLocation().clone());
					mDamageHistory.put(shooter, info);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onFireballShoot(ProjectileLaunchEvent event) {
		if (!isHuntEnabledInWorld(event.getEntity().getWorld()))
			return;

		if (event.getEntity() instanceof Fireball) {
			if (event.getEntity().getShooter() instanceof Blaze) {
				Blaze blaze = (Blaze) event.getEntity().getShooter();
				if (blaze.getTarget() instanceof Player && isHuntEnabled((Player) blaze.getTarget())
						&& ((Player) blaze.getTarget()).getGameMode() != GameMode.CREATIVE) {
					DamageInformation info = mDamageHistory.get(blaze);
					if (info == null)
						info = new DamageInformation();
					info.setTime(System.currentTimeMillis());
					info.setAttacker((Player) blaze.getTarget());
					info.setAttackerPosition(blaze.getTarget().getLocation().clone());
					mDamageHistory.put(blaze, info);
				}
			} else if (event.getEntity().getShooter() instanceof Wither) {
				Wither wither = (Wither) event.getEntity().getShooter();
				if (wither.getTarget() instanceof Player && isHuntEnabled((Player) wither.getTarget())
						&& ((Player) wither.getTarget()).getGameMode() != GameMode.CREATIVE) {
					DamageInformation info = null;
					info = mDamageHistory.get(wither);
					if (info == null)
						info = new DamageInformation();
					info.setTime(System.currentTimeMillis());
					info.setAttacker((Player) wither.getTarget());
					info.setAttackerPosition(wither.getTarget().getLocation().clone());
					mDamageHistory.put(wither, info);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onTriodentShoot(ProjectileLaunchEvent event) {
		if (!Server.isMC113OrNewer())
			return;

		if (event.getEntity() instanceof Trident) {
			if (event.getEntity().getShooter() instanceof Drowned) {
				// TODO: test this
				Drowned drowned = (Drowned) event.getEntity().getShooter();
				if (drowned.getTarget() instanceof Player && isHuntEnabled((Player) drowned.getTarget())
						&& ((Player) drowned.getTarget()).getGameMode() != GameMode.CREATIVE) {
					DamageInformation info = mDamageHistory.get(drowned);
					if (info == null)
						info = new DamageInformation();
					info.setTime(System.currentTimeMillis());
					info.setAttacker((Player) drowned.getTarget());
					info.setAttackerPosition(drowned.getTarget().getLocation().clone());
					mDamageHistory.put(drowned, info);
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onMobDamage(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof LivingEntity) || !isHuntEnabledInWorld(event.getEntity().getWorld()))
			return;// ok
		Entity damager = event.getDamager();
		Entity damaged = event.getEntity();

		// check if damager or damaged is Sentry / Sentinel. Only Sentry gives a
		// reward.
		if (CitizensCompat.isNPC(damaged) && !CitizensCompat.isSentryOrSentinelOrSentries(damaged))
			return;

		if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.WorldGuard.getName()))&& !WorldGuardCompat.isAllowedByWorldGuard(damager, damaged,
				WorldGuardMobHuntingFlag.getMobDamageFlag(), true)) {
			return;
		}

		if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.CrackShot.getName())) && CrackShotCompat.isCrackShotUsed(damaged)) {
			return;
		}

		if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.WeaponMechanics.getName())) && WeaponMechanicsCompat.isWeaponMechanicsWeaponUsed(damaged)) {
			return;
		}

		DamageInformation info = null;
		info = mDamageHistory.get(damaged);
		if (info == null)
			info = new DamageInformation();

		info.setTime(System.currentTimeMillis());

		Player cause = null;
		ItemStack weapon = null;

		if (damager instanceof Player) {
			cause = (Player) damager;
		}

		boolean projectile = false;
		if (damager instanceof Projectile) {
			if (((Projectile) damager).getShooter() instanceof Player)
				cause = (Player) ((Projectile) damager).getShooter();

			if (damager instanceof ThrownPotion)
				weapon = ((ThrownPotion) damager).getItem();
			// else if (damager instanceof Trident)
			// weapon = ((Trident) damager);

			info.setIsMeleWeaponUsed(false);

			projectile = true;

			if (CrackShotCompat.isCrackShotProjectile((Projectile) damager)) {
				info.setCrackShotWeapon(CrackShotCompat.getCrackShotWeapon((Projectile) damager));
			}

			// TODO : Weapon Mechancs Projectile - WM Projectiles is not an entity like in
			// Crackshot.

		} else
			info.setIsMeleWeaponUsed(true);

		if (MyPetCompat.isMyPet(damager)) {
			cause = MyPetCompat.getMyPetOwner(damaged);
			info.setIsMeleWeaponUsed(false);
			info.setIsMyPetAssist(true);
		} else if (damager instanceof Wolf && ((Wolf) damager).isTamed()
				&& ((Wolf) damager).getOwner() instanceof Player) {
			cause = (Player) ((Wolf) damager).getOwner();
			info.setIsMeleWeaponUsed(false);
			info.setIsMyPetAssist(true);
		}

		if (weapon == null && cause != null) {
			if (Server.isMC19OrNewer() && projectile) {
				PlayerInventory pi = cause.getInventory();
				if (pi.getItemInMainHand().getType() == Material.BOW)
					weapon = pi.getItemInMainHand();
				else
					weapon = pi.getItemInOffHand();
			} else {
				weapon = cause.getItemInHand();
			}
			if (CrackShotCompat.isCrackShotWeapon(weapon)) {
				info.setCrackShotWeapon(CrackShotCompat.getCrackShotWeapon(weapon));
				MessageHelper.debug("%s used a CrackShot weapon: %s", cause.getName(),
						info.getCrackShotWeaponUsed());
			}

			if (WeaponMechanicsCompat.isWeaponMechanicsWeapon(weapon)) {
				info.setCrackShotWeapon(WeaponMechanicsCompat.getWeaponMechanicsWeapon(weapon));
				MessageHelper.debug("%s used a Weapon Mechanics weapon: %s", cause.getName(),
						info.getWeaponMechanicsWeapon());
			}
		}

		if (weapon != null)
			info.setWeapon(weapon);

		// Take note that a weapon has been used at all
		if (info.getWeapon() != null && (Materials.isSword(info.getWeapon()) || Materials.isAxe(info.getWeapon())
				|| Materials.isPick(info.getWeapon()) || Materials.isTrident(info.getWeapon())
				|| info.isCrackShotWeaponUsed() || projectile || info.isWeaponMechanicsWeaponUsed()))
			info.setHasUsedWeapon(true);

		if (cause != null) {
			if (cause != info.getAttacker()) {
				info.setAssister(info.getAttacker());
				info.setLastAssistTime(info.getLastAttackTime());
			}

			info.setLastAttackTime(System.currentTimeMillis());

			info.setAttacker(cause);
			if (cause.isFlying() && !cause.isInsideVehicle())
				info.setWasFlying(true);

			info.setAttackerPosition(cause.getLocation().clone());

			if (!info.isPlayerUndercover())
				if (DisguisesHelper.isDisguised(cause)) {
					if (DisguisesHelper.isDisguisedAsAgresiveMob(cause)) {
						MessageHelper.debug("%s was under cover - diguised as an agressive mob",
								cause.getName());
						info.setPlayerUndercover(true);
					} else
						MessageHelper.debug("%s was under cover - diguised as an passive mob",
								cause.getName());
					if (plugin.getConfigManager().removeDisguiseWhenAttacking) {
						DisguisesHelper.undisguiseEntity(cause);
						// if (cause instanceof Player)
						plugin.getMessages().playerActionBarMessageQueue(cause, ChatColor.GREEN + "" + ChatColor.ITALIC
								+ plugin.getMessages().getString("bonus.undercover.message", "cause", cause.getName()));
						if (damaged instanceof Player) {
							plugin.getMessages().playerActionBarMessageQueue((Player) damaged,
									ChatColor.GREEN + "" + ChatColor.ITALIC + plugin.getMessages()
											.getString("bonus.undercover.message", "cause", cause.getName()));
						}
					}
				}

			if (!info.isMobCoverBlown())
				if (DisguisesHelper.isDisguised(damaged)) {
					if (DisguisesHelper.isDisguisedAsAgresiveMob(damaged)) {
						MessageHelper.debug("%s Cover blown, diguised as an agressive mob",
								damaged.getName());
						info.setMobCoverBlown(true);
					} else
						MessageHelper.debug("%s Cover Blown, diguised as an passive mob",
								damaged.getName());
					if (plugin.getConfigManager().removeDisguiseWhenAttacked) {
						DisguisesHelper.undisguiseEntity(damaged);
						if (damaged instanceof Player) {
							plugin.getMessages().playerActionBarMessageQueue((Player) damaged,
									ChatColor.GREEN + "" + ChatColor.ITALIC + plugin.getMessages()
											.getString("bonus.coverblown.message", "damaged", damaged.getName()));
						}
						if (cause instanceof Player) {
							plugin.getMessages().playerActionBarMessageQueue(cause,
									ChatColor.GREEN + "" + ChatColor.ITALIC + plugin.getMessages()
											.getString("bonus.coverblown.message", "damaged", damaged.getName()));
						}
					}
				}

			mDamageHistory.put((LivingEntity) damaged, info);
		}
	}

	long messageLimiter = 0;

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
        private void onMobDeath(EntityDeathEvent event) {

            boolean silent = System.currentTimeMillis() < messageLimiter + 60 * 1000;

            LivingEntity killed = event.getEntity();

            Player killer = event.getEntity().getKiller();

            ExtendedMob mob = plugin.getExtendedMobManager().getExtendedMobFromEntity(killed);
            if (mob.getMob_id() == 0) {
                 MessageHelper.debug("MOB_ID=0");
                return;
            }

		// find player from killer or killed
		Player player = getPlayer(killer, killed);

		DamageInformation info = mDamageHistory.get(killed);
		if (info == null) {
			info = new DamageInformation();
		} else {

		}

        // Only track deaths for grinding detection if we will actually evaluate them.
        if (plugin.getConfigManager().grindingDetectionEnabled
            && !plugin.getGrindingManager().isGrindingDisabledInWorld(killed.getWorld())
            && isHuntEnabledInWorld(killed.getWorld())) {
            plugin.getGrindingManager().registerDeath(killer, killed);
        }

		// Grinding Farm detections
		if (plugin.getConfigManager().grindingDetectionEnabled && plugin.getConfigManager().detectFarms
				&& !plugin.getGrindingManager().isGrindingDisabledInWorld(event.getEntity().getWorld())) {
			if (killed.getLastDamageCause() != null) {
				if (!plugin.getGrindingManager().isWhitelisted(killed.getLocation())) {
					if (killed.getLastDamageCause().getCause() == DamageCause.FALL) {
						if (plugin.getConfigManager().detectNetherGoldFarms) {
							if (!silent)
								MessageHelper
										.debug("=============== NetherGold XP Farm detection ===============");
							if (plugin.getGrindingManager().isNetherGoldXPFarm(killed, silent)) {
								cancelDrops(event, plugin.getConfigManager().disableNaturalItemDropsOnNetherGoldFarms,
										plugin.getConfigManager().disableNaturalXPDropsOnNetherGoldFarms);
								if (player != null) {
									if ((Core.getPlayerSettingsManager().containsKey(player) && Core
											.getPlayerSettingsManager().getPlayerSettings(player).isLearningMode())
											|| player.hasPermission("mobhunting.blacklist")
											|| player.hasPermission("mobhunting.blacklist.show")) {
										if (!silent) {
											plugin.getGrindingManager().showGrindingArea(player, new Area(
													killed.getLocation(),
													plugin.getConfigManager().rangeToSearchForGrinding,
													plugin.getConfigManager().numberOfDeathsWhenSearchingForGringding),
													killed.getLocation());
											plugin.getMessages().learn(player,
													plugin.getMessages().getString("mobhunting.learn.grindingfarm"));
										}
									}
								}
								if (!silent)
									MessageHelper
											.debug("=============== NetherGold XP Farm detection Ended =========");
								return;
							}
							if (!silent)
								MessageHelper
										.debug("=============== NetherGold XP Farm detection Ended =========");

						}
						if (plugin.getConfigManager().detectOtherFarms) {
							if (!silent)
								MessageHelper.debug("=============== Generic Farm detection ===============");
							if (plugin.getGrindingManager().isOtherFarm(killed, silent)) {
								cancelDrops(event, plugin.getConfigManager().disableNaturalItemDropsOnOtherFarms,
										plugin.getConfigManager().disableNaturalXPDropsOnOtherFarms);
								if (player != null) {
									if ((Core.getPlayerSettingsManager().containsKey(player) && Core
											.getPlayerSettingsManager().getPlayerSettings(player).isLearningMode())
											|| player.hasPermission("mobhunting.blacklist.show")
											|| player.hasPermission("mobhunting.blacklist")) {
										if (!silent) {
											plugin.getGrindingManager().showGrindingArea(player, new Area(
													killed.getLocation(),
													plugin.getConfigManager().rangeToSearchForGrindingOnOtherFarms,
													plugin.getConfigManager().numberOfDeathsWhenSearchingForGringdingOnOtherFarms),
													killed.getLocation());
											plugin.getMessages().learn(player,
													plugin.getMessages().getString("mobhunting.learn.grindingfarm"));
										}
									}
								}
								if (!silent)
									MessageHelper
											.debug("=============== Generic Farm detection Ended =========");
								return;
							}
							if (!silent)
								MessageHelper.debug("=============== Generic Farm detection Ended =========");
						}

					} else if (killed.getLastDamageCause().getCause() == DamageCause.VOID) {
						if (plugin.getConfigManager().detectEndermanFarms) {
							if (!silent)
								MessageHelper.debug("=============== Enderman Farm detection ===============");
							if (plugin.getGrindingManager().isEndermanFarm(killed, silent)) {
								cancelDrops(event, plugin.getConfigManager().disableNaturalItemDropsOnEndermanFarms,
										plugin.getConfigManager().disableNaturalXPDropsOnEndermanFarms);
								if (!silent)
									MessageHelper.debug("An enderman died in the void: (%s,%s,%s in %s)",
											killed.getLocation().getX(), killed.getLocation().getY(),
											killed.getLocation().getZ(), killed.getWorld().getName());
								if (player != null) {
									if ((Core.getPlayerSettingsManager().containsKey(player) && Core
											.getPlayerSettingsManager().getPlayerSettings(player).isLearningMode())
											|| player.hasPermission("mobhunting.blacklist.show")
											|| player.hasPermission("mobhunting.blacklist")) {
										if (!silent) {
											plugin.getGrindingManager().showGrindingArea(player, new Area(
													killed.getLocation(),
													plugin.getConfigManager().rangeToSearchForGrindingOnEndermanFarms,
													plugin.getConfigManager().numberOfDeathsWhenSearchingForGringdingOnEndermanFarms),
													killed.getLocation());
											plugin.getMessages().learn(player,
													plugin.getMessages().getString("mobhunting.learn.grindingfarm"));
										}
									}
								}
								if (!silent) {
									MessageHelper
											.debug("=============== Enderman Farm detection Ended =========");
									messageLimiter = System.currentTimeMillis();
								}
								return;
							}
							if (!silent) {
								MessageHelper.debug("=============== Enderman Farm detection Ended =========");
								messageLimiter = System.currentTimeMillis();
							}

						}
					}
				} else {
					// MessageHelper.debug("A mob died in a whitelisted
					// area: (%s,%s,%s in %s)",
					// killed.getLocation().getX(), killed.getLocation().getY(),
					// killed.getLocation().getZ(),
					// killed.getWorld().getName());
				}
			} else {
				// MessageHelper.debug("The %s (%s) died without a
				// damageCause.",
				// mob.getName(), mob.getMobPlugin().getName());
				// return;
			}
		}

		// Killer is not a player and not a MyPet and CrackShot not used.
		if (killer == null && !MyPetCompat.isKilledByMyPet(killed) && !info.isCrackShotWeaponUsed()
				&& !info.isWeaponMechanicsWeaponUsed()) {
			return;
		}

		if (killed != null && (killed.getType() == EntityType.UNKNOWN || killed.getType() == EntityType.ARMOR_STAND)) {
			return;
		}

		MessageHelper.debug("======================== New kill ==========================");

		// Check if the mob was killed by MyPet and assisted_kill is disabled.
		if (killer == null && MyPetCompat.isKilledByMyPet(killed) && plugin.getConfigManager().enableAssists == false) {
			Player owner = MyPetCompat.getMyPetOwner(killed);
			MessageHelper.debug("KillBlocked: %s - Assisted kill is disabled", owner.getName());
			plugin.getMessages().learn(owner,
					plugin.getMessages().getString("mobhunting.learn.assisted-kill-is-disabled"));
			MessageHelper.debug("======================= kill ended (1)======================");
			return;
		}

		// Write killer name to Server Log
		if (killer != null)
			MessageHelper.debug("%s killed a %s (%s)@(%s:%s,%s,%s)", killer.getName(), mob.getMobName(),
					mob.getMobPlugin().getName(), killer.getWorld().getName(), (int) killer.getLocation().getBlockX(),
					(int) killer.getLocation().getBlockY(), (int) killer.getLocation().getBlockZ());
		else if (MyPetCompat.isKilledByMyPet(killed))
			MessageHelper.debug("%s owned by %s killed a %s (%s)@(%s:%s,%s,%s)",
					MyPetCompat.getMyPet(killed).getName(), MyPetCompat.getMyPetOwner(killed).getName(),
					mob.getMobName(), mob.getMobPlugin().getName(),
					MyPetCompat.getMyPetOwner(killed).getWorld().getName(),
					(int) MyPetCompat.getMyPetOwner(killed).getLocation().getBlockX(),
					(int) MyPetCompat.getMyPetOwner(killed).getLocation().getBlockY(),
					(int) MyPetCompat.getMyPetOwner(killed).getLocation().getBlockZ());
		else if (info.isCrackShotWeaponUsed()) {
			if (killer == null) {
				killer = info.getWeaponUser();
				if (killer != null)
					MessageHelper.debug("%s killed a %s (%s) using a %s@ (%s:%s,%s,%s)", killer.getName(),
							mob.getMobName(), mob.getMobPlugin().getName(), info.getCrackShotWeaponUsed(),
							killer.getWorld().getName(), (int) killer.getLocation().getBlockX(),
							(int) killer.getLocation().getBlockY(), (int) killer.getLocation().getBlockZ());
				else
					MessageHelper.debug("No killer was stored in the Damageinformation");
			}
		} else if (info.isWeaponMechanicsWeaponUsed()) {
			if (killer == null) {
				killer = info.getWeaponUser();
				if (killer != null)
					MessageHelper.debug("%s killed a %s (%s) using a %s@ (%s:%s,%s,%s)", killer.getName(),
							mob.getMobName(), mob.getMobPlugin().getName(), info.getWeaponMechanicsWeapon(),
							killer.getWorld().getName(), (int) killer.getLocation().getBlockX(),
							(int) killer.getLocation().getBlockY(), (int) killer.getLocation().getBlockZ());
				else
					MessageHelper.debug("No killer was stored in the Damageinformation");
			}
		}

		// Killer is a NPC
		if (killer != null && CitizensCompat.isNPC(killer)) {
			MessageHelper.debug("KillBlocked: Killer is a Citizen NPC (ID:%s).",
					CitizensCompat.getNPCId(killer));
			MessageHelper.debug("======================= kill ended (2)======================");
			return;
		}

		// Player killed a Citizens2 NPC
		if (player != null && CitizensCompat.isNPC(killed) && CitizensCompat.isSentryOrSentinelOrSentries(killed)) {
			MessageHelper.debug("%s killed a Sentinel, Sentries or a Sentry npc-%s (name=%s)", player.getName(),
					CitizensCompat.getNPCId(killed), mob.getMobName());
		}

		// WorldGuard Compatibility
		if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.WorldGuard.getName()))) {
			if ((killer != null || MyPetCompat.isMyPet(killer)) && !CitizensCompat.isNPC(killer)) {
				if (!WorldGuardCompat.isAllowedByWorldGuard(killer, killed, WorldGuardMobHuntingFlag.getMobDamageFlag(),
						true)) {
					if (WorldGuardCompat.isAllowedByWorldGuard(killer, killed,
							WorldGuardMobHuntingFlag.getMobHuntingFlag(), false)) {
						MessageHelper.debug(
								"KillBlocked: %s is hiding in WG region with mob-damage=DENY, but MobHunting is allowed with flag mobhunting=allow",
								killer.getName());
					} else {
						MessageHelper.debug("KillBlocked: %s is hiding in WG region with mob-damage=DENY",
								killer.getName());
						plugin.getMessages().learn(player,
								plugin.getMessages().getString("mobhunting.learn.mob-damage-flag"));
						cancelDrops(event, plugin.getConfigManager().disableNaturalItemDrops,
								plugin.getConfigManager().disableNatualXPDrops);
						MessageHelper.debug("======================= kill ended (4)======================");
						return;
					}
				} else if (!WorldGuardCompat.isAllowedByWorldGuard(killer, killed,
						WorldGuardMobHuntingFlag.getMobHuntingFlag(), true)) {
					MessageHelper.debug("KillBlocked: %s is in a protected region mobhunting=DENY",
							killer.getName());
					plugin.getMessages().learn(player,
							plugin.getMessages().getString("mobhunting.learn.mobhunting-deny"));
					cancelDrops(event, plugin.getConfigManager().disableNaturalItemDrops,
							plugin.getConfigManager().disableNatualXPDrops);
					MessageHelper.debug("======================= kill ended (5)======================");
					return;
				} else {
					MessageHelper.debug("KillAllowed: Mob killed by %s was allowed by WorldGuard",
							killer.getName());
				}
			}
		}

		// Factions Compatibility - no reward when player are in SafeZone
		if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Factions.getName()))) {
			if ((killer != null || MyPetCompat.isMyPet(killer)) && !CitizensCompat.isNPC(killer)) {
				if (FactionsUUIDCompat.isInHomeZoneAndPeaceful(player)) {
					MessageHelper.debug("KillBlocked: %s is hiding in his Factions Home SafeZone",
							player.getName());
					plugin.getMessages().learn(player,
							plugin.getMessages().getString("mobhunting.learn.factions-no-rewards-in-safezone"));
					cancelDrops(event, plugin.getConfigManager().disableNaturalItemDrops,
							plugin.getConfigManager().disableNatualXPDrops);
					MessageHelper.debug("======================= kill ended (7)======================");
					return;
				} else if (FactionsUUIDCompat.isInSafeZoneAndPeaceful(player)) {
					MessageHelper.debug("KillBlocked: %s is hiding in Factions SafeZone", player.getName());
					plugin.getMessages().learn(player,
							plugin.getMessages().getString("mobhunting.learn.factions-no-rewards-in-safezone"));
					cancelDrops(event, plugin.getConfigManager().disableNaturalItemDrops,
							plugin.getConfigManager().disableNatualXPDrops);
					MessageHelper.debug("======================= kill ended (7.5)======================");
					return;
				}
			}
		}

		// ImprovedFactions Compatibility - no reward when player are in SafeZone
		if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.ImprovedFactions.getName()))) {
			if ((killer != null || MyPetCompat.isMyPet(killer)) && !CitizensCompat.isNPC(killer)) {
				if (ImprovedFactionsCompat.isInHomeZoneAndPeaceful(player)) {
					MessageHelper.debug("KillBlocked: %s is hiding in his Factions Home SafeZone",
							player.getName());
					plugin.getMessages().learn(player,
							plugin.getMessages().getString("mobhunting.learn.factions-no-rewards-in-safezone"));
					cancelDrops(event, plugin.getConfigManager().disableNaturalItemDrops,
							plugin.getConfigManager().disableNatualXPDrops);
					MessageHelper.debug("======================= kill ended (7)======================");
					return;
				} else if (ImprovedFactionsCompat.isInSafeZoneAndPeaceful(player)) {
					MessageHelper.debug("KillBlocked: %s is hiding in Factions SafeZone", player.getName());
					plugin.getMessages().learn(player,
							plugin.getMessages().getString("mobhunting.learn.factions-no-rewards-in-safezone"));
					cancelDrops(event, plugin.getConfigManager().disableNaturalItemDrops,
							plugin.getConfigManager().disableNatualXPDrops);
					MessageHelper.debug("======================= kill ended (7.5)======================");
					return;
				}
			}
		}

        // Towny Compatibility - no reward when player are in a protected town
        if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Towny.getName()))) {
            if ((killer != null || MyPetCompat.isMyPet(killer)) && !CitizensCompat.isNPC(killer)
                    && !(killed instanceof Player)) {
                if (plugin.getConfigManager().disableRewardsInHomeTown && TownyCompat.isInHomeTown(player)) {
                    MessageHelper.debug("KillBlocked: %s is hiding in his home town", player.getName());
                    plugin.getMessages().learn(player,
                            plugin.getMessages().getString("mobhunting.learn.towny-no-rewards-in-home-town"));
                    cancelDrops(event, plugin.getConfigManager().disableNaturallyRewardsInHomeTown,
                            plugin.getConfigManager().disableNaturallyRewardsInHomeTown);
                    MessageHelper.debug("======================= kill ended (8a)======================");
                    return;
                } else if (plugin.getConfigManager().disableRewardsInAnyTown && TownyCompat.isInAnyTown(player)) {
                    MessageHelper.debug("KillBlocked: %s is hiding in a town", player.getName());
                    plugin.getMessages().learn(player,
                            plugin.getMessages().getString("mobhunting.learn.towny-no-rewards-in-any-town"));
                    cancelDrops(event, plugin.getConfigManager().disableNaturallyRewardsInHomeTown,
                            plugin.getConfigManager().disableNaturallyRewardsInHomeTown);
                    MessageHelper.debug("======================= kill ended (8b)======================");
                    return;
                }
            }
        }

		// Residence Compatibility - no reward when player are in a protected
		// residence
		if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Residence.getName()))) {
			if ((killer != null || MyPetCompat.isMyPet(killer)) && !CitizensCompat.isNPC(killer)
					&& !(killed instanceof Player)) {
				if (plugin.getConfigManager().disableRewardsInHomeResidence && ResidenceCompat.isProtected(player)) {
					MessageHelper.debug("KillBlocked: %s is hiding in a protected residence", player.getName());
					plugin.getMessages().learn(player,
							plugin.getMessages().getString("mobhunting.learn.residence-no-rewards-in-protected-area"));
					cancelDrops(event, plugin.getConfigManager().disableNaturallyRewardsInProtectedResidence,
							plugin.getConfigManager().disableNaturallyRewardsInProtectedResidence);
					MessageHelper.debug("======================= kill ended (9)======================");
					return;
				}
			}
		}

		// Check if MobHunting is Disabled in World
		if (!isHuntEnabledInWorld(event.getEntity().getWorld())) {
			if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.WorldGuard.getName()))) {
				if (!CitizensCompat.isNPC(killer) && !MyPetCompat.isKilledByMyPet(killed)) {
					if (WorldGuardCompat.isAllowedByWorldGuard(killer, killed,
							WorldGuardMobHuntingFlag.getMobHuntingFlag(), false)) {
						MessageHelper.debug(
								"KillAllowed %s: Mobhunting disabled in world '%s', but region allows mobhunting=allow",
								player.getName(), player.getWorld().getName());
						plugin.getMessages().learn(player,
								plugin.getMessages().getString("mobhunting.learn.overruled"));
						// OK - continue
					} else {
						MessageHelper.debug("KillBlocked %s: Mobhunting disabled in world '%s'",
								player.getName(), player.getWorld().getName());
						plugin.getMessages().learn(player, plugin.getMessages().getString("mobhunting.learn.disabled"));
						MessageHelper.debug("======================= kill ended (10)======================");
						return;
					}
				} else {
					MessageHelper
							.debug("KillBlocked: killer is null and killer was not a MyPet or NPC Sentinel Guard.");
					MessageHelper.debug("======================= kill ended (11)=====================");
					return;
				}
			} else {
				// MobHunting is NOT allowed in this world,
				MessageHelper.debug("KillBlocked %s: Mobhunting disabled in world '%s'", player.getName(),
						player.getWorld().getName());
				plugin.getMessages().learn(player, plugin.getMessages().getString("mobhunting.learn.disabled"));
				MessageHelper.debug("======================= kill ended (12)=====================");
				return;
			}
		}

		// Handle Muted mode
		boolean killer_muted = false;
		boolean killed_muted = false;
		if (player instanceof Player && Core.getPlayerSettingsManager().containsKey((Player) player))
			killer_muted = Core.getPlayerSettingsManager().getPlayerSettings(player).isMuted();
		if (killed instanceof Player && Core.getPlayerSettingsManager().containsKey((Player) killed))
			killed_muted = Core.getPlayerSettingsManager().getPlayerSettings((Player) killed).isMuted();

		// Player died while playing a Minigame: MobArena,
		// PVPArena,playerGrindingArea.getCenter().getBlock
		// BattleArena, Suicide, PVP, penalty when Mobs kills player
		if (killed instanceof Player) {
			//PVPArena
			if (PVPArenaCompat.isPlayingPVPArena((Player) killed)
					&& !plugin.getConfigManager().pvparenaGetRewards) {
				MessageHelper.debug("KillBlocked: %s was killed while playing PvpArena.", mob.getMobName());
				plugin.getMessages().learn(player, plugin.getMessages().getString("mobhunting.learn.pvparena"));
				MessageHelper.debug("======================= kill ended (14)=====================");
				return;

            // BattleArena
			} else if (BattleArenaCompat.isPlayingBattleArena((Player) killed)) {
				MessageHelper.debug("KillBlocked: %s was killed while playing BattleArena.", mob.getMobName());
				plugin.getMessages().learn(player, plugin.getMessages().getString("mobhunting.learn.battlearena"));
				MessageHelper.debug("======================= kill ended (15)=====================");
				return;

			} else if (MobArenaCompat.isPlayingMobArena((Player) killed) && !plugin.getConfigManager().mobarenaGetRewards) {
                MessageHelper.debug("KillBlocked: %s was killed while playing MobArena.", mob.getMobName());
                plugin.getMessages().learn(player, plugin.getMessages().getString("mobhunting.learn.mobarena"));
                MessageHelper.debug("======================= kill ended (13)=====================");
                return;

                // PVPArena
            } else if (killer != null) {
				if (killed.equals(killer)) {
					// Suicide
					plugin.getMessages().learn(player, plugin.getMessages().getString("mobhunting.learn.suiside"));
					MessageHelper.debug("KillBlocked: Suiside not allowed (Killer=%s, Killed=%s)",
							killer.getName(), killed.getName());
					MessageHelper.debug("======================= kill ended (17)======================");
					return;
					// PVP
				} else if (!plugin.getConfigManager().pvpAllowed) {
					// PVP
					plugin.getMessages().learn(player, plugin.getMessages().getString("mobhunting.learn.nopvp"));
					MessageHelper.debug(
							"KillBlocked: Rewards for PVP kill is not allowed in config.yml. %s killed %s.",
							player.getName(), mob.getMobName());
					MessageHelper.debug("======================= kill ended (18)=====================");
					return;
				}
			}
		}

		// Player killed a mob while playing a minigame: MobArena, PVPVArena,
		// BattleArena
		// Player is in Godmode or Vanished
		// Player permission to Hunt (and get rewards)
		if (PVPArenaCompat.isPlayingPVPArena(player) && !plugin.getConfigManager().pvparenaGetRewards) {
			MessageHelper.debug("KillBlocked: %s is currently playing PvpArena.", player.getName());
			plugin.getMessages().learn(player, plugin.getMessages().getString("mobhunting.learn.pvparena"));
			MessageHelper.debug("======================= kill ended (20)=====================");
			return;
		} else if (MobArenaCompat.isPlayingMobArena(player) && !plugin.getConfigManager().mobarenaGetRewards) {
            MessageHelper.debug("KillBlocked: %s is currently playing MobArena.", player.getName());
            plugin.getMessages().learn(player, plugin.getMessages().getString("mobhunting.learn.mobarena"));
            MessageHelper.debug("======================= kill ended (19)=====================");
            return;
        } else if (BattleArenaCompat.isPlayingBattleArena(player)) {
			MessageHelper.debug("KillBlocked: %s is currently playing BattleArena.", player.getName());
			plugin.getMessages().learn(player, plugin.getMessages().getString("mobhunting.learn.battlearena"));
			MessageHelper.debug("======================= kill ended (21)=====================");
			return;
		} else if (EssentialsCompat.isGodModeEnabled(player)) {
			MessageHelper.debug("KillBlocked: %s is in God mode", player.getName());
			plugin.getMessages().learn(player, plugin.getMessages().getString("mobhunting.learn.godmode"));
			cancelDrops(event, plugin.getConfigManager().disableNaturalItemDrops,
					plugin.getConfigManager().disableNatualXPDrops);
			MessageHelper.debug("======================= kill ended (22)=====================");
			return;
		} else if (EssentialsCompat.isVanishedModeEnabled(player)) {
			MessageHelper.debug("KillBlocked: %s is in Vanished mode", player.getName());
			plugin.getMessages().learn(player, plugin.getMessages().getString("mobhunting.learn.vanished"));
			MessageHelper.debug("======================= kill ended (23)=====================");
			return;
		}

		if (!hasPermissionToKillMob(player, killed)) {
			MessageHelper.debug("KillBlocked: %s has not permission to kill %s.", player.getName(),
					mob.getMobName());
			plugin.getMessages().learn(player,
					plugin.getMessages().getString("mobhunting.learn.no-permission", "killed-mob", mob.getMobName()));
			MessageHelper.debug("======================= kill ended (25a)=====================");
			return;
		}

		if (!plugin.getRewardManager().getMobEnabled(killed)) {
			MessageHelper.debug("KillBlocked: %s is disabled in config.yml", mob.getMobName());
			plugin.getMessages().learn(player,
					plugin.getMessages().getString("mobhunting.learn.mob-disabled", "killed-mob", mob.getMobName()));
			MessageHelper.debug("======================= kill ended (25b)=====================");
			return;
		}

		// Mob Spawner / Egg / Egg Dispenser detection
		if (plugin.getConfigManager().grindingDetectionEnabled
				&& !plugin.getGrindingManager().isGrindingDisabledInWorld(event.getEntity().getWorld())
				&& event.getEntity().hasMetadata(SPAWNER_BLOCKED)) {
			if (!plugin.getGrindingManager().isWhitelisted(event.getEntity().getLocation())) {
				if (killed != null) {
					if (plugin.getConfigManager().enableRewardsFromCaveSpiders
							&& killed.getType().toString().equalsIgnoreCase(MobType.CaveSpider.getMobType())) {
						MessageHelper.debug(
								"%s killed a Cave Spider from a SPAWNER, but this is allowed in config.yml (see enable_rewards_from_cave_spiders)",
								killer.getName());
					} else {
						MessageHelper.debug(
								"KillBlocked: %s(%d) has MH:blocked meta (probably spawned from a mob spawner, an egg or a egg-dispenser) MobType:%s EntityType:%s ",
								event.getEntity().getType(), killed.getEntityId(), mob.getMobtype(),
								event.getEntity().getType());
						plugin.getMessages().learn(player, plugin.getMessages().getString("mobhunting.learn.mobspawner",
								"killed", mob.getMobName()));
						cancelDrops(event,
								plugin.getConfigManager().disableNaturallyDroppedItemsFromMobSpawnersEggsAndDispensers,
								plugin.getConfigManager().disableNaturallyDroppedXPFromMobSpawnersEggsAndDispensers);

						MessageHelper.debug("======================= kill ended (26)======================");
						return;
					}
				}
			} else {
				MessageHelper.debug("A mob from a spawner or an egg was killed in a whitelisted area");
			}
		}

		// MobHunting is disabled for the player
		if (!isHuntEnabled(player)) {
			MessageHelper.debug("KillBlocked: %s Hunting is disabled for player", player.getName());
			plugin.getMessages().learn(player, plugin.getMessages().getString("mobhunting.learn.huntdisabled"));
			MessageHelper.debug("======================= kill ended (27)======================");
			return;
		}

		// The player is in Creative mode
		if (player.getGameMode() == GameMode.CREATIVE) {
			MessageHelper.debug("KillBlocked: %s is in creative mode", player.getName());
			plugin.getMessages().learn(player, plugin.getMessages().getString("mobhunting.learn.creative"));
			cancelDrops(event, plugin.getConfigManager().tryToCancelNaturalDropsWhenInCreative,
					plugin.getConfigManager().tryToCancelXPDropsWhenInCreative);
			MessageHelper.debug("======================= kill ended (28)======================");
			return;
		}

		// Calculate basic the reward
		double cash = plugin.getRewardManager().getBaseKillPrize(killed);
		if (plugin.mRand.nextDouble() > plugin.getRewardManager().getMoneyChance(killed))
			cash = 0;
		double basic_prize = cash;
		MessageHelper.debug("Basic Prize=%s for killing a %s", plugin.getEconomyManager().format(cash),
				mob.getMobName());

		// There is no reward and no penalty for this kill
		if (basic_prize == 0 && plugin.getRewardManager().getKillCommands(killed).isEmpty()
				&& !plugin.getRewardManager().getHeadDropHead(killed)) {
			MessageHelper.debug(
					"KillBlocked %s(%d): There is no reward and no penalty for this Mob/Player and is not counted as kill/achievement.",
					mob.getMobName(), killed.getEntityId());
			plugin.getMessages().learn(player,
					plugin.getMessages().getString("mobhunting.learn.no-reward", "killed", mob.getMobName()));
			MessageHelper.debug("======================= kill ended (29)=====================");
			return;
		}

		// add a multiplier for killing an EliteMob
		if (EliteMobsCompat.isEliteMobs(killed)) {
			int level = EliteMobsCompat.getEliteMobsLevel(killed);
			double mul = 1;
			if (level >= 50)
				mul = plugin.getConfigManager().elitemobMultiplier * (1 + (level - 50) / (400 - 50));
			if (level >= 400)
				mul = plugin.getConfigManager().elitemobMultiplier;
			MessageHelper.debug("A level %s %s EliteMob was killed by %s. Multiplier is %s", level,
					mob.getMobName(), player, mul);
			cash = cash * mul;
		}

		// Update DamageInformation
		if (killed instanceof LivingEntity && mDamageHistory.containsKey((LivingEntity) killed)) {
			info = mDamageHistory.get(killed);
			if (System.currentTimeMillis() - info.getTime() > plugin.getConfigManager().assistTimeout * 1000)
				info = null;
		}
		if (info == null)
			info = new DamageInformation();
		if (info.getWeapon() == null)
			info.setWeapon(new ItemStack(Material.AIR));
		info.setTime(System.currentTimeMillis());
		info.setLastAttackTime(info.getTime());
		if (killer != null) {
			info.setAttacker(player);
			info.setAttackerPosition(player.getLocation());
			@SuppressWarnings("deprecation")
			ItemStack weapon = killer.getItemInHand();
			if (Material.AIR != weapon.getType()) {
				info.setHasUsedWeapon(true);
				if (CrackShotCompat.isCrackShotWeapon(weapon)) {
					info.setCrackShotWeapon(CrackShotCompat.getCrackShotWeapon(weapon));
					MessageHelper.debug("%s used a CrackShot weapon: %s", killer.getName(),
							CrackShotCompat.getCrackShotWeapon(weapon));
				} else if (WeaponMechanicsCompat.isWeaponMechanicsWeapon(weapon)) {
					info.setWeaponMechanicsWeapon(WeaponMechanicsCompat.getWeaponMechanicsWeapon(weapon));
					MessageHelper.debug("%s used a Weapon Mechanings weapon: %s", killer.getName(),
							WeaponMechanicsCompat.getWeaponMechanicsWeapon(weapon));
				} else
					MessageHelper.debug("%s used a weapon: %s", killer.getName(), weapon.getType());
				info.setWeapon(weapon);
			}
		}

		// Check if the kill was within the time limit on both kills and
		// assisted kills
		if (((System.currentTimeMillis() - info.getLastAttackTime()) > plugin.getConfigManager().killTimeout * 1000)
				&& (info.isWolfAssist() && ((System.currentTimeMillis()
						- info.getLastAttackTime()) > plugin.getConfigManager().assistTimeout * 1000))) {
			MessageHelper.debug("KillBlocked %s: Last damage was too long ago (%s sec.)", player.getName(),
					(System.currentTimeMillis() - info.getLastAttackTime()) / 1000);
			MessageHelper.debug("======================= kill ended (30)=====================");
			return;
		}

		// MyPet killed a mob - Assister is the Owner
		if (MyPetCompat.isKilledByMyPet(killed) && plugin.getConfigManager().enableAssists == true) {
			info.setAssister(MyPetCompat.getMyPetOwner(killed));
			MessageHelper.debug("MyPetAssistedKill: Pet owned by %s killed a %s", info.getAssister().getName(),
					mob.getMobName());
		}

		// Player or killed Mob is disguised
		if (!info.isPlayerUndercover())
			if (DisguisesHelper.isDisguised(player)) {
				if (DisguisesHelper.isDisguisedAsAgresiveMob(player)) {
					info.setPlayerUndercover(true);
				} else if (plugin.getConfigManager().removeDisguiseWhenAttacking) {
					DisguisesHelper.undisguiseEntity(player);
					if (player != null && !killer_muted) {
						plugin.getMessages().playerActionBarMessageQueue(player,
								ChatColor.GREEN + "" + ChatColor.ITALIC + plugin.getMessages()
										.getString("bonus.undercover.message", "cause", player.getName()));
					}
					if (killed instanceof Player && !killed_muted) {
						plugin.getMessages().playerActionBarMessageQueue((Player) killed,
								ChatColor.GREEN + "" + ChatColor.ITALIC + plugin.getMessages()
										.getString("bonus.undercover.message", "cause", player.getName()));
					}
				}
			}
		if (!info.isMobCoverBlown())
			if (DisguisesHelper.isDisguised(killed)) {
				if (DisguisesHelper.isDisguisedAsAgresiveMob(killed)) {
					info.setMobCoverBlown(true);
				}
				if (plugin.getConfigManager().removeDisguiseWhenAttacked) {
					DisguisesHelper.undisguiseEntity(killed);
					if (killed instanceof Player && !killed_muted) {
						plugin.getMessages().playerActionBarMessageQueue((Player) killed,
								ChatColor.GREEN + "" + ChatColor.ITALIC + plugin.getMessages()
										.getString("bonus.coverblown.message", "damaged", mob.getMobName()));
					}
					if (player != null && !killer_muted) {
						plugin.getMessages().playerActionBarMessageQueue(player,
								ChatColor.GREEN + "" + ChatColor.ITALIC + plugin.getMessages()
										.getString("bonus.coverblown.message", "damaged", mob.getMobName()));
					}
				}
			}

		HuntData data = new HuntData(player);
		// if (player != null) {
		if (cash != 0 && plugin.getConfigManager().grindingDetectionEnabled
				&& !plugin.getGrindingManager().isGrindingDisabledInWorld(player.getWorld())
				&& plugin.getConfigManager().areaGrindingDetectionEnabled
				&& (!plugin.getGrindingManager().isGrindingArea(player.getLocation())
						|| plugin.getGrindingManager().isWhitelisted(player.getLocation()))
		// && !plugin.getGrindingManager().isPlayerSpeedGrinding(killer, killed)
		) {
			// Killstreak
			if (killed instanceof Slime) {
				// Tiny Slime and MagmaCube do no damage or very little
				// damage, so Killstreak is
				// not achieved if the mob is small
				Slime slime = (Slime) killed;
				if (slime.getSize() != 1)
					data.handleKillstreak(plugin, player);
			} else if (killed instanceof MagmaCube) {
				MagmaCube magmaCube = (MagmaCube) killed;
				if (magmaCube.getSize() != 1)
					data.handleKillstreak(plugin, player);
			} else
				data.handleKillstreak(plugin, player);
		} else {
			// Killstreak ended. Players started to kill 4 chicken and the
			// one mob to gain 4 x prize
			if (data.getKillstreakLevel() != 0 && data.getKillstreakMultiplier() != 1) {
				plugin.getMessages().playerActionBarMessageQueue(player, ChatColor.RED + "" + ChatColor.ITALIC
						+ plugin.getMessages().getString("mobhunting.killstreak.ended"));
			}
			// MessageHelper.debug("%s - Killstreak ended",
			// player.getName());
			MessageHelper.debug("KillStreak was reset to 0");
			data.resetKillStreak(player);
		}

		Location loc = killed.getLocation();

		// first kill for this player
		if (data.getLastKillAreaCenter() == null)
			data.setLastKillAreaCenter(loc.clone());

		// Grinding detection
		if (plugin.getConfigManager().grindingDetectionEnabled
				&& !(cash == 0 && plugin.getRewardManager().getKillCommands(killed).isEmpty())) {

			// Disable grinding detection i specific worlds
			if (!plugin.getGrindingManager().isGrindingDisabledInWorld(player.getWorld())) {

				// Area Grinding detection
				if (plugin.getConfigManager().areaGrindingDetectionEnabled) {
					// Check if Area is whitelisted
					if (plugin.getGrindingManager().isWhitelisted(loc)) {
						MessageHelper.debug("This Area is whitelisted. Area grinding not detected.");

					} else if (StackMobCompat.isGrindingStackedMobsAllowed() && StackMobCompat.isStackedMob(killed)) {
						MessageHelper.debug(
								"The killed mob was a Stacked Mob and Grinding is allowed in config.yml. Area grinding and speed grinding is not deteted.");

					} else {

						data.setDampenedKills(data.getDampenedKills() + 1);

						// Check if the location is Blacklisted as a Grinding Area by Admin
						Area grindingArea = plugin.getGrindingManager().getGrindingArea(loc);
						if (plugin.getGrindingManager().isGrindingArea(loc)) {
							plugin.getGrindingManager().showGrindingArea(killer, grindingArea, killed.getLocation());
							data.resetKillStreak(player);
							MessageHelper.debug(
									"Blacklisted grinding area detected Center=(%s,%s,%s). No rewards is paid.",
									grindingArea.getCenter().getBlock(), grindingArea.getCenter().getBlockX(),
									grindingArea.getCenter().getBlockZ());
							plugin.getMessages().learn(player,
									plugin.getMessages().getString("mobhunting.learn.grindingnotallowed"));
							cancelDrops(event, plugin.getConfigManager().disableNaturalItemDropsOnPlayerGrinding,
									plugin.getConfigManager().disableNaturalXPDropsOnPlayerGrinding);
							MessageHelper.debug("======================= kill ended (31)=====================");
							return;
						}

						// Check the is a player specific grinding area
						Area playerGrindingArea = data.getPlayerGrindingArea(loc);
						if (playerGrindingArea != null) {
							plugin.getGrindingManager().showGrindingArea(killer, playerGrindingArea,
									killed.getLocation());
							data.resetKillStreak(player);
							MessageHelper.debug(
									"%s has been registered for grinding in this area. Center=(%s,%s,%s)",
									player.getName(), playerGrindingArea.getCenter().getBlockX(),
									playerGrindingArea.getCenter().getBlockY(),
									playerGrindingArea.getCenter().getBlockZ());
							plugin.getMessages().learn(player,
									plugin.getMessages().getString("mobhunting.learn.grindingnotallowed"));
							cancelDrops(event, plugin.getConfigManager().disableNaturalItemDropsOnPlayerGrinding,
									plugin.getConfigManager().disableNaturalXPDropsOnPlayerGrinding);
							MessageHelper.debug("======================= kill ended (32)=====================");
							return;
						}

						int maxKills = (isSlimeOrMagmaCube(killed) ? 2 : 1)
								* plugin.getConfigManager().grindingDetectionNumberOfDeath;

						// The mob was killed far from last kill area center
						if (!loc.getWorld().equals(data.getLastKillAreaCenter().getWorld())
								|| loc.distance(data.getLastKillAreaCenter()) > data.getcDampnerRange()) {

							MessageHelper.debug(
									"Kill not within %s blocks from previous kill. Dampened Kills reset to 0",
									data.getcDampnerRange());
							data.setDampenedKills(0);
							data.setLastKillAreaCenter(loc.clone());

							// Grinding stacked mobs
						} else {

							MessageHelper.debug(
									"Checking kills in this area. DampenedKills=%s. Penalty begins at %s. Max is %s",
									data.getDampenedKills(), maxKills / 2, maxKills);

							// Player has reached the limit
							if (data.getDampenedKills() >= maxKills) {

								data.setLastKillAreaCenter(loc.clone());
								data.recordPlayerGrindingArea();
								data.resetKillStreak(player);

								playerGrindingArea = data.getPlayerGrindingArea(loc.clone());

								if (plugin.getConfigManager().blacklistPlayerGrindingSpotsServerWorldWide)
									plugin.getGrindingManager().registerKnownGrindingSpot(playerGrindingArea);

								MessageHelper.debug(
										"Area Grinding detected Center=(%s,%s,%s,%s). %s has reached the limit:%s of allowed kills in this area",
										data.getLastKillAreaCenter().getWorld().getName(),
										data.getLastKillAreaCenter().getBlockX(),
										data.getLastKillAreaCenter().getBlockY(),
										data.getLastKillAreaCenter().getBlockZ(), player.getName(), maxKills);
								plugin.getMessages().learn(player,
										plugin.getMessages().getString("mobhunting.learn.grindingnotallowed"));
								plugin.getMessages().playerActionBarMessageQueue(player,
										ChatColor.RED + plugin.getMessages().getString("mobhunting.grinding.detected"));
								plugin.getGrindingManager().showGrindingArea(player, playerGrindingArea, loc);
								cancelDrops(event, plugin.getConfigManager().disableNaturalItemDrops,
										plugin.getConfigManager().disableNatualXPDrops);

								// Check if player is above ½ of the limit
							} else if (data.getDampenedKills() >= maxKills / 2) {
								MessageHelper.debug(
										"Warning: %s is killing too many mobs. Player is above half of the limit: %s ",
										player.getName(), maxKills);
								plugin.getMessages().learn(player,
										plugin.getMessages().getString("mobhunting.learn.grindingnotallowed"));
								plugin.getMessages().playerActionBarMessageQueue(player,
										ChatColor.RED + plugin.getMessages().getString("mobhunting.grinding.detected"));

								// Player is below ½ of the limit
							} else {
								MessageHelper.debug("DampenedKills are below half of limit: %s",
										plugin.getConfigManager().grindingDetectionNumberOfDeath);
							}
						}

						data.setLastKillAreaCenter(loc.clone());
						data.putHuntDataToPlayer(player);
					}
				} else {
					MessageHelper
							.debug("Area Grinding detection is disabled in config.yml (detect_grinding_areas).");
				}
				if (plugin.getConfigManager().speedGrindingDetectionEnabled) {
					if (plugin.getGrindingManager().isPlayerSpeedGrinding(killer, killed)) {
						MessageHelper.debug("%s is Speed Grinding. No rewards paid", player.getName());
						if (data.getKillstreakLevel() != 0 && data.getKillstreakMultiplier() != 1) {
							plugin.getMessages().playerActionBarMessageQueue(player,
									ChatColor.RED + plugin.getMessages().getString("mobhunting.killstreak.lost"));
						}
						MessageHelper.debug("KillStreak reset to 0");
						data.setKillStreak(0);
						cancelDrops(event, plugin.getConfigManager().disableNaturalItemDropsOnPlayerGrinding,
								plugin.getConfigManager().disableNaturalXPDropsOnPlayerGrinding);
						MessageHelper.debug("======================= kill ended (34)======================");
						return;

					} else {
						MessageHelper.debug("%s is not Speed Grinding.", player.getName());
					}
				} else {
					MessageHelper.debug("Speed Grinding is disabled in config.yml (detect_speed_grinding).");
				}
			} else {
				MessageHelper.debug(
						"Grinding detection in world:%s is disabled in config.yml (disable_grinding_detection_in_worlds).",
						player.getWorld().getName());
			}
		} else {
			MessageHelper.debug(
					"Grinding detection is disabled in config.yml (enable_grinding_detection) or there is no reward for this Mob.");
		}

		// Apply the modifiers to Basic reward
		EntityDamageByEntityEvent lastDamageCause = null;
		if (killed.getLastDamageCause() instanceof EntityDamageByEntityEvent)
			lastDamageCause = (EntityDamageByEntityEvent) killed.getLastDamageCause();
		double multipliers = 1.0;
		ArrayList<String> modifiers = new ArrayList<String>();
		// only add modifiers if the killer is the player.
		for (IModifier mod : mHuntingModifiers) {
			if (mod.doesApply(killed, player, data, info, lastDamageCause)) {
				double amt = mod.getMultiplier(killed, player, data, info, lastDamageCause);
				if (amt != 1.0) {
					modifiers.add(mod.getName());
					multipliers *= amt;
					data.addModifier(mod.getName(), amt);
					MessageHelper.debug("Multiplier: %s = %s", mod.getName(), amt);
				}
			}
		}
		data.setReward(cash);
		data.putHuntDataToPlayer(player);

		MessageHelper.debug("Killstreak=%s, level=%s, multiplier=%s ", data.getKillStreak(),
				data.getKillstreakLevel(), data.getKillstreakMultiplier());
		multipliers *= data.getKillstreakMultiplier();

		String extraString = "";

		// Only display the multiplier if its not 1
		if (Math.abs(multipliers - 1) > 0.05)
			extraString += String.format("x%.1f", multipliers);

		// Add on modifiers
		for (String modifier : modifiers)
			extraString += ChatColor.WHITE + " * " + modifier;

		cash *= multipliers;

		cash = Tools.ceil(cash);

		// Handle Bounty Kills
		double reward = 0;
		if (plugin.getConfigManager().enablePlayerBounties && killed instanceof Player) {
			MessageHelper.debug("This was a PVP kill (killed=%s), number of bounties=%s", killed.getName(),
					plugin.getBountyManager().getAllBounties().size());
			OfflinePlayer wantedPlayer = (OfflinePlayer) killed;
			String worldGroupName = Core.getWorldGroupManager().getCurrentWorldGroup(player);
			if (plugin.getBountyManager().hasOpenBounties(wantedPlayer)) {
				BountyKillEvent bountyEvent = new BountyKillEvent(worldGroupName, player, wantedPlayer,
						plugin.getBountyManager().getOpenBounties(worldGroupName, wantedPlayer));
				Bukkit.getPluginManager().callEvent(bountyEvent);
				if (bountyEvent.isCancelled()) {
					MessageHelper.debug("KillBlocked %s: BountyKillEvent was cancelled",
							(killer != null ? killer : info.getAssister()).getName());
					MessageHelper.debug("======================= kill ended (35)=====================");
					return;
				}
				Set<Bounty> bounties = plugin.getBountyManager().getOpenBounties(worldGroupName, wantedPlayer);
				for (Bounty b : bounties) {
					reward += b.getPrize();
					OfflinePlayer bountyOwner = b.getBountyOwner();
					plugin.getBountyManager().delete(b);
					if (bountyOwner != null && bountyOwner.isOnline()) {
						plugin.getMessages().playerActionBarMessageQueue(Tools.getOnlinePlayer(bountyOwner),
								plugin.getMessages().getString("mobhunting.bounty.bounty-claimed", "killer",
										player.getName(), "prize", plugin.getEconomyManager().format(b.getPrize()),
										"money", plugin.getEconomyManager().format(b.getPrize()), "killed",
										killed.getName()));
					}
					b.setStatus(BountyStatus.completed);
					plugin.getDataStoreManager().updateBounty(b);
				}
				plugin.getMessages().playerActionBarMessageQueue(player,
						plugin.getMessages().getString("mobhunting.moneygain-for-killing", "prize",
								plugin.getEconomyManager().format(reward), "money",
								plugin.getEconomyManager().format(reward), "killed", killed.getName()));
				MessageHelper.debug("Bounty: %s got %s for killing %s", player.getName(), reward,
						killed.getName());
				plugin.getRewardManager().depositPlayer(player, reward);
				// MessageHelper.debug("RecordCash: %s killed a %s (%s)
				// Cash=%s",
				// killer.getName(), mob.getName(),
				// mob.getMobPlugin().name(), cash);
				// plugin.getDataStoreManager().recordCash(killer, mob,
				// killed.hasMetadata("MH:hasBonus"), cash);

			} else {
				MessageHelper.debug("There is no Bounty on %s", killed.getName());
			}
		}

		cash = Tools.round(cash);
		MessageHelper.debug("Reward rounded to %s", cash);

		// Check if there is a reward for this kill
		if ((cash >= Core.getConfigManager().minimumReward && cash != 0)
				|| (cash <= -Core.getConfigManager().minimumReward && cash != 0)
				|| !plugin.getRewardManager().getKillCommands(killed).isEmpty()
				|| (killer != null && MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.mcMMO.getName())) && plugin.getConfigManager().enableMcMMOLevelRewards)
				|| plugin.getRewardManager().getHeadDropHead(killed)) {

			// Remember: Handle MobHuntKillEvent and Record Hunt Achievement is
			// done using
			// EighthsHuntAchievement.java (onKillCompleted)
			MobHuntKillEvent event2 = new MobHuntKillEvent(data, info, killed, player);
			Bukkit.getPluginManager().callEvent(event2);
			// Check if Event is cancelled before paying the reward
			if (event2.isCancelled()) {
				MessageHelper.debug("KillBlocked %s: MobHuntKillEvent was cancelled", player.getName());
				MessageHelper.debug("======================= kill ended (36)=====================");
				return;
			}

			// Record the kill in the Database
			if (info.getAssister() == null || plugin.getConfigManager().enableAssists == false) {
				MessageHelper.debug("RecordKill: %s killed a %s (%s) Cash=%s", player.getName(),
						mob.getMobName(), mob.getMobPlugin().name(), plugin.getEconomyManager().format(cash));
				plugin.getDataStoreManager().recordKill(player, mob, killed.hasMetadata("MH:hasBonus"), cash);
			} else {
				if (MyPetCompat.isKilledByMyPet(killed))
					MessageHelper.debug("RecordAssistedKill: %s killed a %s (%s) Cash=%s",
							player.getName() + "/" + MyPetCompat.getMyPet(killed).getName(), mob.getMobName(),
							mob.getMobPlugin().name(), plugin.getEconomyManager().format(cash));

				else
					MessageHelper.debug("RecordAssistedKill: %s killed a %s (%s) Cash=%s",
							player.getName() + "/" + info.getAssister().getName(), mob.getMobName(),
							mob.getMobPlugin().name(), plugin.getEconomyManager().format(cash));
				plugin.getDataStoreManager().recordAssist(player, killer, mob, killed.hasMetadata("MH:hasBonus"), cash);
			}
		} else {
			MessageHelper.debug("KillBlocked %s: There is now reward for killing a %s", player.getName(),
					mob.getMobName());
			MessageHelper.debug("======================= kill ended (37)=====================");
			return;
		}

		String worldname = player.getWorld().getName();
		String killerpos = player.getLocation().getBlockX() + " " + player.getLocation().getBlockY() + " "
				+ player.getLocation().getBlockZ();
		String killedpos = killed.getLocation().getBlockX() + " " + killed.getLocation().getBlockY() + " "
				+ killed.getLocation().getBlockZ();

		// send a message to the player
		if (!plugin.getRewardManager().getKillMessage(killed).trim().isEmpty() && !killer_muted) {
			String message = Strings.convertColors(ChatColor.GREEN + plugin.getRewardManager().getKillMessage(killed)
					.trim().replaceAll("\\{player\\}", player.getName()).replaceAll("\\{killer\\}", player.getName())
					.replaceAll("\\{killed\\}", mob.getLocalizedName())
					.replaceAll("\\{prize\\}", plugin.getEconomyManager().format(cash))
					.replaceAll("\\{money\\}", plugin.getEconomyManager().format(cash))
					.replaceAll("\\{world\\}", worldname).replaceAll("\\{killerpos\\}", killerpos)
					.replaceAll("\\{killedpos\\}", killedpos)
					.replaceAll("\\{rewardname\\}", Core.getConfigManager().bagOfGoldName.trim()));
			if (killed instanceof Player)
				message = message.replaceAll("\\{killed_player\\}", killed.getName()).replaceAll("\\{killed\\}",
						killed.getName());
			else
				message = message.replaceAll("\\{killed_player\\}", mob.getMobName()).replaceAll("\\{killed\\}",
						mob.getMobName());
			MessageHelper.debug("Message to be send to player:" + message);
			plugin.getMessages().playerSendMessageAt(player, message,
					MessageType.valueOf(plugin.getConfigManager().defaultMessageType));
		}

		// Pay the money reward to killer/player and assister
		if ((cash >= Core.getConfigManager().minimumReward || cash <= -Core.getConfigManager().minimumReward)
				&& cash != 0) {

			// Handle reward on PVP kill. (Robbing)
			boolean robbing = killer != null && killed instanceof Player && !CitizensCompat.isNPC(killed)
					&& plugin.getConfigManager().pvpAllowed && plugin.getConfigManager().robFromVictim;
			if (robbing) {
				MessageHelper.debug("PVP kill reward is '%s'", plugin.getConfigManager().pvpKillMoney);
				if (cash > plugin.getEconomyManager().getBalance((Player) killed)) {
					MessageHelper.debug("The killed player has only %s in his pockets to steel",
							plugin.getEconomyManager().getBalance((Player) killed));
					cash = plugin.getEconomyManager().getBalance((Player) killed);
				}
				plugin.getEconomyManager().withdrawPlayer((Player) killed, cash);
				// MessageHelper.debug("RecordCash: %s killed a %s (%s)
				// Cash=%s",
				// killer.getName(), mob.getName(),
				// mob.getMobPlugin().name(), cash);
				// plugin.getDataStoreManager().recordCash(killer, mob,
				// killed.hasMetadata("MH:hasBonus"), -cash);
				if (!killed_muted)
					killed.sendMessage(ChatColor.RED + "" + ChatColor.ITALIC
							+ plugin.getMessages().getString("mobhunting.moneylost", "prize",
									plugin.getEconomyManager().format(cash), "money",
									plugin.getEconomyManager().format(cash)));
				MessageHelper.debug("%s lost %s", killed.getName(), plugin.getEconomyManager().format(cash));
			}

			// Reward/Penalty for assisted kill
			if (info.getAssister() == null || plugin.getConfigManager().enableAssists == false) {
				if (cash >= Core.getConfigManager().minimumReward && cash != 0) {
					if (plugin.getConfigManager().dropMoneyOnGround) {
						plugin.getRewardManager().dropMoneyOnGround_RewardManager(killer, killed, killed.getLocation(),
								cash);
					} else {
						plugin.getRewardManager().depositPlayer(killer, cash);
						// MessageHelper.debug("RecordCash: %s killed a
						// %s (%s)
						// Cash=%s", killer.getName(), mob.getName(),
						// mob.getMobPlugin().name(), cash);
						// plugin.getDataStoreManager().recordCash(killer,
						// mob, killed.hasMetadata("MH:hasBonus"), cash);
						MessageHelper.debug("%s got a reward (%s)", killer.getName(),
								plugin.getEconomyManager().format(cash));
					}
				} else if (cash <= -Core.getConfigManager().minimumReward && cash != 0) {
					plugin.getRewardManager().withdrawPlayer(killer, -cash);
					// MessageHelper.debug("RecordCash: %s killed a %s
					// (%s) Cash=%s",
					// killer.getName(), mob.getName(),
					// mob.getMobPlugin().name(), cash);
					// plugin.getDataStoreManager().recordCash(killer, mob,
					// killed.hasMetadata("MH:hasBonus"), cash);
					MessageHelper.debug("%s got a penalty (%s)", killer.getName(),
							plugin.getEconomyManager().format(cash));
				}
			} else {
				cash = Tools.round(cash / 2);
				if (cash >= Core.getConfigManager().minimumReward & cash != 0) {
					if (plugin.getConfigManager().dropMoneyOnGround) {
						if (MyPetCompat.isKilledByMyPet(killed))
							MessageHelper.debug("1)%s was assisted by %s. Reward/Penalty is only ½ (%s)",
									player.getName(), MyPetCompat.getMyPet(killed).getName(),
									plugin.getEconomyManager().format(cash));
						else if (CitizensCompat.isNPC(killer))
							MessageHelper.debug("2)%s was assisted by %s. Reward/Penalty is only ½ (%s)",
									player.getName(), killer.getName(), plugin.getEconomyManager().format(cash));
						else
							MessageHelper.debug("3)%s was assisted by %s. Reward/Penalty is only ½ (%s)",
									player.getName(), getKillerName(killer, killed),
									plugin.getEconomyManager().format(cash));
						plugin.getRewardManager().dropMoneyOnGround_RewardManager(player, killed, killed.getLocation(),
								cash);
					} else {
						plugin.getRewardManager().depositPlayer(info.getAssister(), cash);

						if (!MyPetCompat.isKilledByMyPet(killed) && !CitizensCompat.isNPC(killer))
							onAssist(player, killer, killed, info.getLastAssistTime());
						if (MyPetCompat.isKilledByMyPet(killed))
							MessageHelper.debug("%s was assisted by %s. Reward/Penalty is only ½ (%s)",
									player.getName(), MyPetCompat.getMyPet(killed).getName(),
									plugin.getEconomyManager().format(cash));
						else
							MessageHelper.debug("%s was assisted by %s. Reward/Penalty is only ½ (%s)",
									player.getName(), getKillerName(killer, killed),
									plugin.getEconomyManager().format(cash));
					}
				} else if (cash <= -Core.getConfigManager().minimumReward && cash != 0) {
					plugin.getRewardManager().withdrawPlayer(player, -cash);
					if (!MyPetCompat.isKilledByMyPet(killed) && !CitizensCompat.isNPC(killer))
						onAssist(info.getAssister(), killer, killed, info.getLastAssistTime());
					MessageHelper.debug("%s was assisted by %s. Reward/Penalty is only ½ (%s)", player.getName(),
							getKillerName(killer, killed), plugin.getEconomyManager().format(cash));
				}
			}

			// Tell the player that he got the reward/penalty, unless muted
			if (!killer_muted) {
				MessageType messageType = MessageType.valueOf(plugin.getConfigManager().defaultMoneyMessageType);
				if (extraString.trim().isEmpty()) {
					if (cash >= Core.getConfigManager().minimumReward && cash != 0) {
						if (!plugin.getConfigManager().dropMoneyOnGround) {
							plugin.getMessages().playerSendMessageAt(player, ChatColor.GREEN + "" + ChatColor.ITALIC
									+ plugin.getMessages().getString("mobhunting.moneygain", "prize",
											plugin.getEconomyManager().format(cash), "money",
											plugin.getEconomyManager().format(cash), "killed", mob.getLocalizedName()),
									messageType);
						} else
							plugin.getMessages().playerSendMessageAt(player, ChatColor.GREEN + "" + ChatColor.ITALIC
									+ plugin.getMessages().getString("mobhunting.moneygain.drop", "prize",
											plugin.getEconomyManager().format(cash), "money",
											plugin.getEconomyManager().format(cash), "killed", mob.getLocalizedName()),
									messageType);
					} else if (cash <= -Core.getConfigManager().minimumReward && cash != 0) {
						plugin.getMessages().playerSendMessageAt(player, ChatColor.RED + "" + ChatColor.ITALIC
								+ plugin.getMessages().getString("mobhunting.moneylost", "prize",
										plugin.getEconomyManager().format(cash), "money",
										plugin.getEconomyManager().format(cash), "killed", mob.getLocalizedName()),
								messageType);
					}

				} else {
					if (cash >= Core.getConfigManager().minimumReward && cash != 0) {
						if (!plugin.getConfigManager().dropMoneyOnGround) {
							plugin.getMessages().playerSendMessageAt(player, ChatColor.GREEN + "" + ChatColor.ITALIC
									+ plugin.getMessages().getString("mobhunting.moneygain.bonuses", "basic_prize",
											plugin.getEconomyManager().format(basic_prize), "prize",
											plugin.getEconomyManager().format(cash), "money",
											plugin.getEconomyManager().format(cash), "bonuses", extraString.trim(),
											"multipliers", plugin.getEconomyManager().format(multipliers), "killed",
											mob.getLocalizedName()),
									messageType);
						} else
							plugin.getMessages().playerSendMessageAt(player, ChatColor.GREEN + "" + ChatColor.ITALIC
									+ plugin.getMessages().getString("mobhunting.moneygain.bonuses.drop", "basic_prize",
											plugin.getEconomyManager().format(basic_prize), "prize",
											plugin.getEconomyManager().format(cash), "money",
											plugin.getEconomyManager().format(cash), "bonuses", extraString.trim(),
											"multipliers", plugin.getEconomyManager().format(multipliers), "killed",
											mob.getLocalizedName()),
									messageType);
					} else if (cash <= -Core.getConfigManager().minimumReward && cash != 0) {
						plugin.getMessages().playerSendMessageAt(player,
								ChatColor.RED + "" + ChatColor.ITALIC
										+ plugin.getMessages().getString("mobhunting.moneylost.bonuses", "basic_prize",
												plugin.getEconomyManager().format(basic_prize), "prize",
												plugin.getEconomyManager().format(cash), "money",
												plugin.getEconomyManager().format(cash), "bonuses", extraString.trim(),
												"multipliers", multipliers, "killed", mob.getLocalizedName()),
								messageType);
					}
				}
			}
		} else
			MessageHelper.debug("The money reward was 0 or less than 'minimum_reward: %s'  (Bonuses=%s)",
					player.getName(), Core.getConfigManager().minimumReward, extraString);

		// McMMO Level rewards
		if (killer != null && MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.mcMMO.getName())) && plugin.getConfigManager().enableMcMMOLevelRewards
				&& data.getDampenedKills() < 10 && !CrackShotCompat.isCrackShotUsed(killed)
				&& !WeaponMechanicsCompat.isWeaponMechanicsWeaponUsed(killed)) {

			String skilltypename = McMMOCompat.getSkilltypeName(info);

			if (skilltypename != null) {
				double chance = plugin.mRand.nextDouble();
				MessageHelper.debug("If %s<%s %s will get a McMMO Level for %s", chance,
						plugin.getRewardManager().getMcMMOChance(killed), killer.getName(), skilltypename);

				if (chance < plugin.getRewardManager().getMcMMOChance(killed)) {
					int level = plugin.getRewardManager().getMcMMOLevel(killed);
					McMMOCompat.addLevel(killer, skilltypename, level);
					MessageHelper.debug("%s was rewarded with %s McMMO Levels for skill %s", killer.getName(),
							plugin.getRewardManager().getMcMMOLevel(killed), skilltypename);
					killer.sendMessage(plugin.getMessages().getString("mobhunting.mcmmo.skilltype_level", "mcmmo_level",
							level, "skilltype", skilltypename));
				}
			}
		}

		// Run console commands as a reward
		if (data.getDampenedKills() < 10) {
			Iterator<HashMap<String, String>> itr = plugin.getRewardManager().getKillCommands(killed).iterator();
			while (itr.hasNext()) {
				HashMap<String, String> cmd = itr.next();
				String perm = cmd.getOrDefault("permission", "");
				if (perm.isEmpty() || player.hasPermission(perm)) {
					double randomNumber = plugin.mRand.nextDouble();
					double chance = 0;
					try {
						chance = Double.valueOf(cmd.get("chance"));
					} catch (Exception e) {
						MessageHelper.error("The chance to run a command when killing a " + killed.getName()
										+ " must be formatted as a string. Ex. chance: '0.5'");
					}
					if (randomNumber < chance) {
						String commandCmd = "";
						try {
							commandCmd = cmd.getOrDefault("cmd", "");
						} catch (Exception e1) {
							MessageHelper.debug(
									"ERROR in config.yml. Could not read cmd for %s. 'cmd' must be a String or ''",
									mob.getMobName());
						}
						if (commandCmd != null) {
							commandCmd = commandCmd.replaceAll("\\{player\\}", player.getName())
									.replaceAll("\\{killer\\}", player.getName())
									.replaceAll("\\{killed\\}", mob.getMobName()).replaceAll("\\{world\\}", worldname)
									.replaceAll("\\{prize\\}", plugin.getEconomyManager().format(cash))
									.replaceAll("\\{money\\}", plugin.getEconomyManager().format(cash))
									.replaceAll("\\{killerpos\\}", killerpos).replaceAll("\\{killedpos\\}", killedpos)
									.replaceAll("\\{rewardname\\}", Core.getConfigManager().bagOfGoldName.trim());
							if (killed instanceof Player)
								commandCmd = commandCmd.replaceAll("\\{killed_player\\}", killed.getName())
										.replaceAll("\\{killed\\}", killed.getName());
							else
								commandCmd = commandCmd.replaceAll("\\{killed_player\\}", mob.getMobName())
										.replaceAll("\\{killed\\}", mob.getMobName());
							MessageHelper.debug("Command to be run:" + commandCmd);
							if (!commandCmd.isEmpty()) {
								String str = commandCmd;
								do {
									if (str.contains("|")) {
										int n = str.indexOf("|");
										try {
											Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
													str.substring(0, n));
										} catch (CommandException e) {
											MessageHelper.error("Could not run cmd:\""
															+ str.substring(0, n) + "\" when Mob:" + mob.getMobName()
															+ " was killed by " + player.getName());
                                            MessageHelper.error("Command:" + str.substring(0, n));
										}
										str = str.substring(n + 1, str.length());
									}
								} while (str.contains("|"));
								try {
									Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), str);
								} catch (CommandException e) {
                                    MessageHelper.error("Could not run cmd:\""
													+ str + "\" when Mob:" + mob.getMobName() + " was killed by "
													+ player.getName());
                                    MessageHelper.error("Command:" + str);
								}
							}
						}
						MessageType messageType = MessageType
								.valueOf((cmd == null || cmd.get("message_type") == null) ? "Chat"
										: cmd.getOrDefault("message_type", "Chat"));
						String message = cmd.getOrDefault("message", "");
						if (message != null && !killer_muted) {
							plugin.getMessages().playerSendMessageAt(player,
									message.replaceAll("\\{player\\}", player.getName())
											.replaceAll("\\{killer\\}", player.getName())
											.replaceAll("\\{killed\\}", mob.getLocalizedName())
											.replaceAll("\\{world\\}", worldname)
											.replaceAll("\\{prize\\}", plugin.getEconomyManager().format(cash))
											.replaceAll("\\{money\\}", plugin.getEconomyManager().format(cash))
											.replaceAll("\\{killerpos\\}", killerpos)
											.replaceAll("\\{killedpos\\}", killedpos).replaceAll("\\{rewardname\\}",
													Core.getConfigManager().bagOfGoldName.trim()),
									messageType);
						}
					} else
						MessageHelper.debug(
								"The command did not run because random number (%s) was bigger than chance (%s)",
								randomNumber, cmd.get("chance"));
				} else {
					MessageHelper.debug("%s has not permission (%s) to run command: %s", player.getName(),
							cmd.get("permission"), cmd.get("cmd"));
				}
			}

			// Update PlaceHolderData
			if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.PlaceholderAPI.getName()))) {
				if (info.getAssister() == null) {
					PlaceHolderData p = PlaceholderAPICompat.getPlaceHolders().get(player.getUniqueId());
					if (p != null) {
						p.setTotal_kills(p.getTotal_kills() + 1);
						PlaceholderAPICompat.getPlaceHolders().put(player.getUniqueId(), p);
					}
				} else {
					PlaceHolderData p = PlaceholderAPICompat.getPlaceHolders().get(player.getUniqueId());
					if (p != null) {
						p.setTotal_assists(p.getTotal_assists() + 1);
						PlaceholderAPICompat.getPlaceHolders().put(player.getUniqueId(), p);
					}
				}
			}

		}

		// drop a head if allowed
		if (plugin.getRewardManager().getHeadDropHead(killed)) {
			double random = plugin.mRand.nextDouble();
			if (random < plugin.getRewardManager().getHeadDropChance(killed)) {
				MobType minecraftMob = MobType.getMobType(killed);
				if (minecraftMob == MobType.PvpPlayer) {
					ItemStack head = CoreCustomItems.getPlayerHead(killed.getUniqueId(), killed.getName(),
							1, plugin.getRewardManager().getHeadValue(killed));
					player.getWorld().dropItem(killed.getLocation(), head);
				} else {
					ItemStack head = CoreCustomItems.getCustomHead(minecraftMob, mob.getEntityName(), 1,
							plugin.getRewardManager().getHeadValue(killed), minecraftMob.getSkinUUID());
					player.getWorld().dropItem(killed.getLocation(), head);
				}
				MessageHelper.debug("%s killed a %s and a head was dropped (random: %s<%s config)",
						getKillerName(killer, killed), killed.getName(), random,
						plugin.getRewardManager().getHeadDropChance(killed));
				if (!plugin.getRewardManager().getHeadDropMessage(killed).isEmpty()) {
					MessageType message_type = MessageType.valueOf(plugin.getConfigManager().defaultHeadMessageType);
					plugin.getMessages().playerSendMessageAt(killer,
							ChatColor.GREEN + Strings.convertColors(plugin.getRewardManager().getHeadDropMessage(killed)
									.replaceAll("\\{player\\}", player.getName())
									.replaceAll("\\{killer\\}", player.getName())
									.replaceAll("\\{killed\\}", mob.getLocalizedName())
									.replaceAll("\\{prize\\}", plugin.getEconomyManager().format(cash))
									.replaceAll("\\{money\\}", plugin.getEconomyManager().format(cash))
									.replaceAll("\\{world\\}", worldname).replaceAll("\\{killerpos\\}", killerpos)
									.replaceAll("\\{killedpos\\}", killedpos)
									.replaceAll("\\{rewardname\\}", Core.getConfigManager().bagOfGoldName.trim())),
							message_type);
				}
			} else {
				MessageHelper.debug("Did not drop a head: random(%s)>chance(%s)", random,
						plugin.getRewardManager().getHeadDropChance(killed));
			}
		}

		MessageHelper.debug("======================= kill ended (38)=====================");
	}

	private boolean isSlimeOrMagmaCube(Entity entity) {
		return entity instanceof Slime || entity instanceof MagmaCube;
	}

	/**
	 * Get the Player or the MyPet owner (Player)
	 *
	 * @param killer - the player who killed the mob
	 * @param killed - the mob which died
	 * @return the Player or return null when killer is not a player and killed not
	 *         killed by a MyPet.
	 */
	private Player getPlayer(Player killer, Entity killed) {
		if (killer != null)
			return killer;

		if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.MyPet.getName()))) {
			Player owner = MyPetCompat.getMyPetOwner(killed);
			if (owner != null)
				return owner;
		}

		DamageInformation damageInformation = mDamageHistory.get(killed);
		if (damageInformation != null
				&& (damageInformation.isCrackShotWeaponUsed() || damageInformation.isWeaponMechanicsWeaponUsed()))
			return damageInformation.getAttacker();

		// MessageHelper.debug("MobHuntingManager: Name of killer was not
		// found. Killer=%s, killed=%s", killer,
		// killed);

		return null;

	}

	private String getKillerName(Player killer, Entity killed) {
		if (killer != null)
			return killer.getName();
		if (MyPetCompat.isKilledByMyPet(killed))
			return MyPetCompat.getMyPet(killed).getName();
		else
			return "";
	}

	private void cancelDrops(EntityDeathEvent event, boolean items, boolean xp) {
		if (items) {
			MessageHelper.debug("Removing naturally dropped items");
			event.getDrops().clear();
		}
		if (xp) {
			MessageHelper.debug("Removing naturally dropped XP");
			event.setDroppedExp(0);
		}
	}

	private void onAssist(Player player, Player killer, LivingEntity killed, long time) {
		if (!plugin.getConfigManager().enableAssists
				|| (System.currentTimeMillis() - time) > plugin.getConfigManager().assistTimeout * 1000)
			return;

		double multiplier = plugin.getConfigManager().assistMultiplier;
		double ks = 1.0;
		if (plugin.getConfigManager().assistAllowKillstreak) {
			HuntData data = new HuntData(player);
			ks = data.handleKillstreak(plugin, player);
		}

		multiplier *= ks;
		double cash = 0;
		if (killed instanceof Player)
			cash = plugin.getRewardManager().getBaseKillPrize(killed) * multiplier / 2;
		else
			cash = plugin.getRewardManager().getBaseKillPrize(killed) * multiplier;

		if ((cash >= Core.getConfigManager().minimumReward || cash <= -Core.getConfigManager().minimumReward)
				&& cash != 0) {
			ExtendedMob mob = plugin.getExtendedMobManager().getExtendedMobFromEntity(killed);
			if (mob.getMob_id() == 0) {
				MessageHelper.error("Unknown Mob:" + mob.getMobName()
						+ " from plugin " + mob.getMobPlugin());
				MessageHelper.error("Please report this to developer!");
				return;
			}
			// plugin.getDataStoreManager().recordAssist(player, killer,
			// mob, killed.hasMetadata("MH:hasBonus"), cash);
			if (cash >= 0)
				plugin.getRewardManager().depositPlayer(player, cash);
			else
				plugin.getRewardManager().withdrawPlayer(player, -cash);
			// MessageHelper.debug("RecordCash: %s killed a %s (%s)
			// Cash=%s",
			// killer.getName(), mob.getName(),
			// mob.getMobPlugin().name(), cash);
			// plugin.getDataStoreManager().recordCash(killer, mob,
			// killed.hasMetadata("MH:hasBonus"), cash);
			MessageHelper.debug("%s got a on assist reward (%s)", player.getName(),
					plugin.getEconomyManager().format(cash));

			if (ks != 1.0)
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.GREEN + "" + ChatColor.ITALIC
								+ plugin.getMessages().getString("mobhunting.moneygain.assist", "prize",
										plugin.getEconomyManager().format(cash), "money",
										plugin.getEconomyManager().format(cash)));
			else {
				plugin.getMessages().playerActionBarMessageQueue(player, ChatColor.GREEN + "" + ChatColor.ITALIC
						+ plugin.getMessages().getString("mobhunting.moneygain.assist.bonuses", "prize",
								plugin.getEconomyManager().format(cash), "money",
								plugin.getEconomyManager().format(cash), "bonuses", String.format("x%.1f", ks)));
			}
		} else
			MessageHelper.debug("KillBlocked %s: Reward was less than 'minimum_reward: %s' or 0",
					killer.getName(), Core.getConfigManager().minimumReward);
		;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void bonusMobSpawn(CreatureSpawnEvent event) {
		// Bonus Mob can't be Citizens and MyPet
		if (CitizensCompat.isNPC(event.getEntity()) || MyPetCompat.isMyPet(event.getEntity()))
			return;

		if (event.getEntityType() == EntityType.ENDER_DRAGON)
			return;

		if (event.getEntityType() == EntityType.CREEPER)
			return;

		if (!isHuntEnabledInWorld(event.getLocation().getWorld())
				|| (plugin.getRewardManager().getBaseKillPrize(event.getEntity()) == 0
						&& plugin.getRewardManager().getKillCommands(event.getEntity()).isEmpty())
				|| event.getSpawnReason() != SpawnReason.NATURAL)
			return;

		if (plugin.mRand.nextDouble() * 100 < plugin.getConfigManager().bonusMobChance) {
			plugin.getParticleManager().attachEffect(event.getEntity(), Effect.MOBSPAWNER_FLAMES);
			if (plugin.mRand.nextBoolean())
				event.getEntity()
						.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 3));
			else
				event.getEntity().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2));
			event.getEntity().setMetadata("MH:hasBonus", new FixedMetadataValue(plugin, true));
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void spawnerMobSpawn(CreatureSpawnEvent event) {
		// Citizens and MyPet can't be spawned from Spawners and eggs
		if (CitizensCompat.isNPC(event.getEntity()) || MyPetCompat.isMyPet(event.getEntity()))
			return;

		if (!isHuntEnabledInWorld(event.getLocation().getWorld())
				|| (plugin.getRewardManager().getBaseKillPrize(event.getEntity()) == 0)
						&& plugin.getRewardManager().getKillCommands(event.getEntity()).isEmpty())
			return;

		if (event.getSpawnReason() == SpawnReason.SPAWNER || event.getSpawnReason() == SpawnReason.SPAWNER_EGG
				|| event.getSpawnReason() == SpawnReason.DISPENSE_EGG) {
			if (plugin.getConfigManager().disableMoneyRewardsFromMobSpawnersEggsAndDispensers)
				if (plugin.getConfigManager().grindingDetectionEnabled
						&& !plugin.getGrindingManager().isWhitelisted(event.getEntity().getLocation()))
					event.getEntity().setMetadata(SPAWNER_BLOCKED, new FixedMetadataValue(plugin, true));
		}
		// if (event.getEntityType().equals(EntityType.MAGMA_CUBE))
		// MessageHelper.debug("MobHuntingManager: a Magma Cube was spawned. The
		// spawnreason is %s",
		// event.getSpawnReason());
		// result is: MobHuntingManager: a Magma Cube was spawned. The spawnreason is
		// SLIME_SPLIT
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void spawnerMobSpawn(SlimeSplitEvent event) {
		if (plugin.getConfigManager().denySlimesToSpiltIfFromSpawer
				&& (event.getEntityType().equals(EntityType.MAGMA_CUBE)
						|| event.getEntityType().equals(EntityType.SLIME))) {
			if (event.getEntity().hasMetadata(SPAWNER_BLOCKED)) {
				MessageHelper.debug(
						"[Splitting Blocked] Splitting i s blocked because the Slime/Magma Cube from a SPAWNER / SPAWNEGG. This behavior can be changed in Config.yml.");
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void reinforcementMobSpawn(CreatureSpawnEvent event) {

		if (event.getSpawnReason() != SpawnReason.REINFORCEMENTS)
			return;

		LivingEntity mob = event.getEntity();

		if (CitizensCompat.isNPC(mob) && !CitizensCompat.isSentryOrSentinelOrSentries(mob))
			return;

		if (!isHuntEnabledInWorld(event.getLocation().getWorld())
				|| (plugin.getRewardManager().getBaseKillPrize(mob) <= 0)
						&& plugin.getRewardManager().getKillCommands(mob).isEmpty())
			return;

		event.getEntity().setMetadata("MH:reinforcement", new FixedMetadataValue(plugin, true));

	}

	public Set<IModifier> getHuntingModifiers() {
		return mHuntingModifiers;
	}

	public WeakHashMap<LivingEntity, DamageInformation> getDamageHistory() {
		return mDamageHistory;
	}
}

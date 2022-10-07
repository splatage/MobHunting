package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import me.deecaad.weaponmechanics.WeaponMechanicsAPI;
import one.lindegaard.Core.compatibility.CompatPlugin;
import one.lindegaard.MobHunting.DamageInformation;
import one.lindegaard.MobHunting.MobHunting;

public class WeaponMechanicsCompat implements Listener {

	private static Plugin mPlugin;
	private static boolean supported = false;


	public WeaponMechanicsCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getLogger().info(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Compatibility with WeaponMechanics is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(CompatPlugin.WeaponMechanics.getName());

			if (mPlugin.getDescription().getVersion().compareTo("1.11") >= 0) {

				Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
						+ "Enabling compatibility with WeaponMechanics (" + mPlugin.getDescription().getVersion() + ")");

				supported = true;

				Bukkit.getPluginManager().registerEvents(this, MobHunting.getInstance());

			} else {
				Bukkit.getConsoleSender()
						.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RED
								+ "Your current version of WeaponMechanics (" + mPlugin.getDescription().getVersion()
								+ ") has no API implemented. Please update to V1.11 or newer.");
			}
		}
	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public Plugin getPlugin() {
		return mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationWeaponMechanics;
	}
	
	public static String getWeaponName(ItemStack itemStack) {
		return WeaponMechanicsAPI.getWeaponTitle(itemStack);
	}
	
/**
	public static boolean isCrackShotWeapon(ItemStack itemStack) {
		if (isSupported()) {
			CSUtility cs = new CSUtility();
			return cs.getWeaponTitle(itemStack) != null;
		}
		return false;
	}

	public static String getCrackShotWeapon(ItemStack itemStack) {
		if (isSupported()) {
			CSUtility cs = new CSUtility();
			return cs.getWeaponTitle(itemStack);
		}
		return null;
	}

	public static boolean isCrackShotProjectile(Projectile Projectile) {
		if (isSupported()) {
			CSUtility cs = new CSUtility();
			return cs.getWeaponTitle(Projectile) != null;
		}
		return false;
	}

	public static String getCrackShotWeapon(Projectile Projectile) {
		if (isSupported()) {
			CSUtility cs = new CSUtility();
			return cs.getWeaponTitle(Projectile);
		}
		return null;
	}

	public static boolean isCrackShotUsed(Entity entity) {
		if (MobHunting.getInstance().getMobHuntingManager().getDamageHistory().containsKey(entity))
			return MobHunting.getInstance().getMobHuntingManager().getDamageHistory().get(entity)
					.getCrackShotWeaponUsed() != null
					&& !MobHunting.getInstance().getMobHuntingManager().getDamageHistory().get(entity)
							.getCrackShotWeaponUsed().isEmpty();
		return false;
	}
**/
	// **************************************************************************
	// EVENTS
	// **************************************************************************

	/**
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.LOW)
	public void onWeaponDamageEntityEvent(WeaponDamageEntityEvent event) {
		if (event.getVictim() instanceof LivingEntity) {
			DamageInformation info = MobHunting.getInstance().getMobHuntingManager().getDamageHistory()
					.get(event.getVictim());
			if (info == null)
				info = new DamageInformation();
			info.setTime(System.currentTimeMillis());
			info.setAttacker(event.getPlayer());
			info.setAttackerPosition(event.getPlayer().getLocation().clone());
			info.setCrackShotWeapon(getCrackShotWeapon(event.getPlayer().getItemInHand()));
			info.setCrackShotPlayer(event.getPlayer());
			MobHunting.getInstance().getMobHuntingManager().getDamageHistory().put((LivingEntity) event.getVictim(),
					info);
		}
	}
	**/

}

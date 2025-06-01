package metadev.digital.MetaMobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import me.deecaad.weaponmechanics.WeaponMechanicsAPI;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponAssistEvent;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponDamageEntityEvent;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponKillEntityEvent;
import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.compatibility.CompatPlugin;
import metadev.digital.MetaMobHunting.DamageInformation;
import metadev.digital.MetaMobHunting.MobHunting;

public class WeaponMechanicsCompat implements Listener {

	private static Plugin mPlugin;
	private static boolean supported = false;
	private final String latestSupported = "1.11";

	public WeaponMechanicsCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender().sendMessage(
					MobHunting.PREFIX_WARNING + "Compatibility with WeaponMechanics is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(CompatPlugin.WeaponMechanics.getName());

			if (mPlugin.getDescription().getVersion().compareTo(latestSupported) >= 0) {

				Bukkit.getConsoleSender()
						.sendMessage(MobHunting.PREFIX + "Enabling compatibility with WeaponMechanics ("
								+ mPlugin.getDescription().getVersion() + ")");

				supported = true;

				Bukkit.getPluginManager().registerEvents(this, MobHunting.getInstance());

			} else {
				Bukkit.getConsoleSender()
						.sendMessage(MobHunting.PREFIX_WARNING + "Your current version of WeaponMechanics ("
								+ mPlugin.getDescription().getVersion()
								+ ") is not supported by MobHunting. Please upgrade to " + latestSupported + " or newer.");
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

	public static String getWeaponMechanicsWeapon(ItemStack itemStack) {
		return WeaponMechanicsAPI.getWeaponTitle(itemStack);
	}

	public static boolean isWeaponMechanicsWeapon(ItemStack itemStack) {
		if (isSupported()) {
			String weaponName = WeaponMechanicsAPI.getWeaponTitle(itemStack);
			if (weaponName != null) {
				Core.getMessages().debug("This is a WeaponMechanics weapon: %s", weaponName);
				return true;
			}
		}
		return false;
	}

	public static boolean isWeaponMechanicsWeaponUsed(Entity entity) {
		if (MobHunting.getInstance().getMobHuntingManager().getDamageHistory().containsKey(entity))
			return MobHunting.getInstance().getMobHuntingManager().getDamageHistory().get(entity)
					.getCrackShotWeaponUsed() != null
					&& !MobHunting.getInstance().getMobHuntingManager().getDamageHistory().get(entity)
							.getWeaponMechanicsWeapon().isEmpty();
		return false;
	}

	/**
	 * public static boolean isWeaponMechanicsProjectile(Projectile Projectile) { if
	 * (isSupported()) { return WeaponMechanicsAPI.getWeaponTitle(Projectile) !=
	 * null; } return false; }
	 * 
	 * public static String getWeaponMechanicsWeapon(Projectile Projectile) { if
	 * (isSupported()) { return WeaponMechanicsAPI.getWeaponTitle(Projectile); }
	 * return null; }
	 **/
	// **************************************************************************
	// EVENTS
	// **************************************************************************

	@EventHandler(priority = EventPriority.LOW)
	public void onWeaponDamageEntityEvent(WeaponDamageEntityEvent event) {
		if (event.getVictim() instanceof LivingEntity) {
			DamageInformation info = MobHunting.getInstance().getMobHuntingManager().getDamageHistory()
					.get(event.getVictim());
			if (info == null)
				info = new DamageInformation();
			info.setTime(System.currentTimeMillis());
			info.setAttacker((Player) event.getShooter());
			info.setAttackerPosition(event.getShooter().getLocation().clone());
			info.setCrackShotWeapon(getWeaponMechanicsWeapon(((Player) event.getShooter()).getItemInHand()));
			info.setWeaponUser((Player) event.getShooter());
			MobHunting.getInstance().getMobHuntingManager().getDamageHistory().put(event.getVictim(), info);
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onWeaponKillEntityEvent(WeaponKillEntityEvent event) {
		// TESTING
		LivingEntity victim = event.getVictim();
		Player player = (Player) event.getShooter();
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onWeaponAssistEvent(WeaponAssistEvent event) {
		// TESTING
		LivingEntity victim = event.getKilled();
		// Player player = (Player) event.getAssistInfo().;
	}

}

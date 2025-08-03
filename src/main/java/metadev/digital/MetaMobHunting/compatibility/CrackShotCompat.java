package metadev.digital.MetaMobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.shampaggon.crackshot.CSUtility;
import com.shampaggon.crackshot.events.WeaponDamageEntityEvent;

import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.DamageInformation;
import metadev.digital.MetaMobHunting.MobHunting;

public class CrackShotCompat implements Listener {

	private static Plugin mPlugin;
	private static boolean supported = false;
	private final String latestSupported = "0.98.5";
	// https://dev.bukkit.org/projects/crackshot
	// API: https://github.com/Shampaggon/CrackShot/wiki/Hooking-into-CrackShot

	public CrackShotCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender()
					.sendMessage(MobHunting.PREFIX_WARNING + "Compatibility with CrackShot is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.CrackShot.getName());

			if (mPlugin.getDescription().getVersion().compareTo(latestSupported) >= 0) {

				Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX + "Enabling compatibility with CrackShot ("
						+ mPlugin.getDescription().getVersion() + ")");

				supported = true;

				Bukkit.getPluginManager().registerEvents(this, MobHunting.getInstance());

			} else {
				Bukkit.getConsoleSender()
						.sendMessage(MobHunting.PREFIX_WARNING + "Your current version of CrackShot ("
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
		return MobHunting.getInstance().getConfigManager().enableIntegrationCrackShot;
	}

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

	// **************************************************************************
	// EVENTS
	// **************************************************************************

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
			info.setWeaponUser(event.getPlayer());
			MobHunting.getInstance().getMobHuntingManager().getDamageHistory().put((LivingEntity) event.getVictim(),
					info);
		}
	}

}

package metadev.digital.MetaMobHunting.modifier;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import metadev.digital.MetaMobHunting.DamageInformation;
import metadev.digital.MetaMobHunting.HuntData;
import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.compatibility.WeaponMechanicsCompat;

public class WeaponMechanicsPenalty implements IModifier {

	@Override
	public String getName() {
		return ChatColor.LIGHT_PURPLE + MobHunting.getInstance().getMessages().getString("bonus.weaponmechanics.name");
	}

	@Override
	public double getMultiplier(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		return MobHunting.getInstance().getConfigManager().weaponMechanicsShot;
	}

	@Override
	public boolean doesApply(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		return WeaponMechanicsCompat.isWeaponMechanicsWeaponUsed(deadEntity);
	}

}

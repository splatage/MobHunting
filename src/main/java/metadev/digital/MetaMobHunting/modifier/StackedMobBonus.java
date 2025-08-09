package metadev.digital.MetaMobHunting.modifier;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import metadev.digital.MetaMobHunting.DamageInformation;
import metadev.digital.MetaMobHunting.HuntData;
import metadev.digital.MetaMobHunting.MobHunting;
// TODO: POSSIBLY DEPRECATED import metadev.digital.MetaMobHunting.compatibility.MobStackerCompat;
import metadev.digital.MetaMobHunting.compatibility.StackMobCompat;
import metadev.digital.MetaMobHunting.compatibility.StackMobHelper;

public class StackedMobBonus implements IModifier {

	@Override
	public String getName() {
		return ChatColor.AQUA + MobHunting.getInstance().getMessages().getString("bonus.mobstacker.name");
	}

	@Override
	public double getMultiplier(Entity entity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		/** // TODO: POSSIBLY DEPRECATED  if (MobStackerCompat.isSupported() && MobStackerCompat.killHoleStackOnDeath(entity)
				&& MobStackerCompat.multiplyLoot()) {
			MobHunting.getInstance().getMessages().debug("StackedMobBonus: Pay reward for no %s mob",
					MobStackerCompat.getStackSize(entity));
			return MobStackerCompat.getStackSize(entity);
		} else */if (StackMobCompat.isSupported() && StackMobHelper.killHoleStackOnDeath(entity)) {
			MessageHelper.debug("StackedMobBonus: Pay reward for no %s mob",
					StackMobHelper.getStackSize((LivingEntity) entity));
			return StackMobHelper.getStackSize((LivingEntity) entity);
		} else {
			MessageHelper.debug("StackedMobBonus: Pay reward for one mob");
			return 1;
		}
	}

	@Override
	public boolean doesApply(Entity entity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		// TODO: POSSIBLY DEPRECATED return MobStackerCompat.isStackedMob(entity) || StackMobHelper.isStackedMob((LivingEntity) entity);
		return StackMobHelper.isStackedMob((LivingEntity) entity);
	}
}

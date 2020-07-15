package one.lindegaard.MobHunting.modifier;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import one.lindegaard.MobHunting.DamageInformation;
import one.lindegaard.MobHunting.HuntData;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.compatibility.MobStackerCompat;
import one.lindegaard.MobHunting.compatibility.StackMobCompat;
import one.lindegaard.MobHunting.compatibility.StackMobHelper;

public class StackedMobBonus implements IModifier {

	@Override
	public String getName() {
		return ChatColor.AQUA + MobHunting.getInstance().getMessages().getString("bonus.mobstacker.name");
	}

	@Override
	public double getMultiplier(Entity entity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		if (MobStackerCompat.isSupported() && MobStackerCompat.killHoleStackOnDeath(entity)
				&& MobStackerCompat.multiplyLoot()) {
			MobHunting.getInstance().getMessages().debug("StackedMobBonus: Pay reward for no %s mob",
					MobStackerCompat.getStackSize(entity));
			return MobStackerCompat.getStackSize(entity);
		} else if (StackMobCompat.isSupported() && StackMobHelper.killHoleStackOnDeath(entity)) {
			MobHunting.getInstance().getMessages().debug("StackedMobBonus: Pay reward for no %s mob",
					StackMobHelper.getStackSize((LivingEntity) entity));
			return StackMobHelper.getStackSize((LivingEntity) entity);
		} else {
			MobHunting.getInstance().getMessages().debug("StackedMobBonus: Pay reward for one mob");
			return 1;
		}
	}

	@Override
	public boolean doesApply(Entity entity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		return MobStackerCompat.isStackedMob(entity) || StackMobHelper.isStackedMob((LivingEntity) entity);
	}
}

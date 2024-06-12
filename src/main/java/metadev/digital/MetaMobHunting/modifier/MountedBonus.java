package metadev.digital.MetaMobHunting.modifier;

import org.bukkit.ChatColor;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import metadev.digital.MetaMobHunting.DamageInformation;
import metadev.digital.MetaMobHunting.HuntData;
import metadev.digital.MetaMobHunting.MobHunting;

public class MountedBonus implements IModifier {

	@Override
	public String getName() {
		return ChatColor.GOLD + MobHunting.getInstance().getMessages().getString("bonus.mounted.name"); //$NON-NLS-1$
	}

	@Override
	public double getMultiplier(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		return MobHunting.getInstance().getConfigManager().bonusMounted;
	}

	@Override
	public boolean doesApply(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		if (killer != null && killer.isInsideVehicle() && killer.getVehicle() instanceof Creature)
			return true;

		return false;
	}

}

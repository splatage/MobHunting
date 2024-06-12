package metadev.digital.MetaMobHunting.modifier;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import metadev.digital.MetaMobHunting.DamageInformation;
import metadev.digital.MetaMobHunting.HuntData;
import metadev.digital.MetaMobHunting.MobHunting;

public class ShoveBonus implements IModifier {

	@Override
	public String getName() {
		return ChatColor.AQUA + MobHunting.getInstance().getMessages().getString("bonus.ashove.name"); //$NON-NLS-1$
	}

	@Override
	public double getMultiplier(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		return MobHunting.getInstance().getConfigManager().bonusSendFalling;
	}

	@Override
	public boolean doesApply(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		if (extraInfo.getAttacker() != killer)
			return false;

		if (deadEntity.getLastDamageCause() != null)
			return deadEntity.getLastDamageCause().getCause() == DamageCause.FALL;
		return false;
	}

}

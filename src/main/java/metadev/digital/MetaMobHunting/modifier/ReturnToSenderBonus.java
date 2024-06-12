package metadev.digital.MetaMobHunting.modifier;

import org.bukkit.ChatColor;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import metadev.digital.MetaMobHunting.DamageInformation;
import metadev.digital.MetaMobHunting.HuntData;
import metadev.digital.MetaMobHunting.MobHunting;

public class ReturnToSenderBonus implements IModifier {

	@Override
	public String getName() {
		return ChatColor.GOLD + MobHunting.getInstance().getMessages().getString("bonus.returntosender.name");
	}

	@Override
	public double getMultiplier(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		return MobHunting.getInstance().getConfigManager().bonusReturnToSender;
	}

	@Override
	public boolean doesApply(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		if (!(deadEntity instanceof Ghast))
			return false;
		if (!(deadEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent))
			return false;

		return (((EntityDamageByEntityEvent) deadEntity.getLastDamageCause()).getDamager() instanceof LargeFireball);
	}

}

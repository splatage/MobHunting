package metadev.digital.MetaMobHunting.modifier;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import metadev.digital.MetaMobHunting.DamageInformation;
import metadev.digital.MetaMobHunting.HuntData;
import metadev.digital.MetaMobHunting.MobHunting;

public class BonusMobBonus implements IModifier {

	@Override
	public String getName() {
		return MobHunting.getInstance().getMessages().getString("bonus.bonusmob.name");
	}

	@Override
	public double getMultiplier(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		return MobHunting.getInstance().getConfigManager().bonusBonusMob;
	}

	@Override
	public boolean doesApply(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		return deadEntity.hasMetadata("MH:hasBonus");
	}

}

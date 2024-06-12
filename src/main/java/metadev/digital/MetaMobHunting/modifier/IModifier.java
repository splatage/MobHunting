package metadev.digital.MetaMobHunting.modifier;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import metadev.digital.MetaMobHunting.DamageInformation;
import metadev.digital.MetaMobHunting.HuntData;

public interface IModifier {
	public String getName();

	public double getMultiplier(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause);

	public boolean doesApply(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause);
}

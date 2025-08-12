package metadev.digital.MetaMobHunting.modifier;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import metadev.digital.MetaMobHunting.DamageInformation;
import metadev.digital.MetaMobHunting.HuntData;
import metadev.digital.MetaMobHunting.MobHunting;

public class WorldBonus implements IModifier {

	@Override
	public String getName() {
		return MobHunting.getInstance().getMessages().getString("bonus.world.name");
	}

	@Override
	public double getMultiplier(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {

		if (MobHunting.getInstance().getConfigManager().worldMultiplier.containsKey(killer.getWorld().getName())) {
			try {
				return Double.valueOf(
						MobHunting.getInstance().getConfigManager().worldMultiplier.get(killer.getWorld().getName()));
			} catch (Exception e) {
				return 1;
			}
		} else
			return 1;
	}

	@Override
	public boolean doesApply(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		if (MobHunting.getInstance().getConfigManager().worldMultiplier.containsKey(killer.getWorld().getName())) {
			try {
				return Double.valueOf(MobHunting.getInstance().getConfigManager().worldMultiplier
						.get(killer.getWorld().getName())) != 1;
			} catch (Exception e) {
                MessageHelper.error("Error in World multiplier for world:" + killer.getWorld().getName());

				return false;
			}
		} else
			return false;
	}

}

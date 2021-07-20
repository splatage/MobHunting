package one.lindegaard.MobHunting.modifier;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import one.lindegaard.MobHunting.DamageInformation;
import one.lindegaard.MobHunting.HuntData;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.compatibility.LevelledMobsCompat;

public class LevelledMobsBonus implements IModifier {

	@Override
	public String getName() {
		return MobHunting.getInstance().getMessages().getString("bonus.levelledmobs.name");
	}

	@Override
	public double getMultiplier(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		MobHunting.getInstance().getMessages().debug("LevelledMobs total multiplier = %s",
				Math.pow(MobHunting.getInstance().getConfigManager().mulitiplierPerLevel,
						LevelledMobsCompat.getLevelledMobsLevel(deadEntity) - 1));
		return Math.pow(MobHunting.getInstance().getConfigManager().mulitiplierPerLevel,
				LevelledMobsCompat.getLevelledMobsLevel(deadEntity) - 1);
	}

	@Override
	public boolean doesApply(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		//MobHunting.getInstance().getMessages().debug("%s killed a LevelledMob %s", killer.getName(),
		//		deadEntity.getType());
		return LevelledMobsCompat.isLevelledMobs(deadEntity);
	}

}

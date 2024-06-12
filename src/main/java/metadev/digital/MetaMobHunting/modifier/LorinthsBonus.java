package metadev.digital.MetaMobHunting.modifier;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import metadev.digital.MetaMobHunting.DamageInformation;
import metadev.digital.MetaMobHunting.HuntData;
import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.compatibility.LorinthsRpgMobsCompat;

public class LorinthsBonus implements IModifier {

	@Override
	public String getName() {
		return MobHunting.getInstance().getMessages().getString("bonus.lorinthsrpgmobs.name");
	}

	@Override
	public double getMultiplier(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		MobHunting.getInstance().getMessages().debug("LorinthsRpgMobs total multiplier = %s", Math.pow(
				MobHunting.getInstance().getConfigManager().mulitiplierPerLevel, LorinthsRpgMobsCompat.getLorinthsRpgMobsLevel(deadEntity)-1));
		return Math.pow(MobHunting.getInstance().getConfigManager().mulitiplierPerLevel,
				LorinthsRpgMobsCompat.getLorinthsRpgMobsLevel(deadEntity)-1);
	}

	@Override
	public boolean doesApply(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		//MobHunting.getInstance().getMessages().debug("%s killed a LorinthsRpgMobs %s level %s", killer.getName(), deadEntity.getType(),
		//		LorinthsRpgMobsCompat.getLorinthsRpgMobsLevel(deadEntity));
		return deadEntity.hasMetadata(LorinthsRpgMobsCompat.MH_LORINTHS_RPG_MOBS);
	}

}

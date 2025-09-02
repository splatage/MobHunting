package metadev.digital.MetaMobHunting.modifier;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import metadev.digital.MetaMobHunting.DamageInformation;
import metadev.digital.MetaMobHunting.HuntData;
import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.compatibility.addons.LevelledMobsCompat;

public class LevelledMobsBonus implements IModifier {

	@Override
	public String getName() {
		return MobHunting.getInstance().getMessages().getString("bonus.levelledmobs.name");
	}

	@Override
	public double getMultiplier(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		if(MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.LevelledMobs.getName()))) {
			MessageHelper.debug("LevelledMobs total multiplier = %s",
					Math.pow(MobHunting.getInstance().getConfigManager().mulitiplierPerLevel,
							LevelledMobsCompat.getLevelledMobsLevel(deadEntity) - 1));
			return Math.pow(MobHunting.getInstance().getConfigManager().mulitiplierPerLevel,
					LevelledMobsCompat.getLevelledMobsLevel(deadEntity) - 1);
		}else return 1;
	}

	@Override
	public boolean doesApply(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		return LevelledMobsCompat.isLevelledMobs(deadEntity);
	}

}

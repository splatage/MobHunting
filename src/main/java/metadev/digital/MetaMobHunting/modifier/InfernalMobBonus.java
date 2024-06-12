package metadev.digital.MetaMobHunting.modifier;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import metadev.digital.MetaMobHunting.DamageInformation;
import metadev.digital.MetaMobHunting.HuntData;
import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.compatibility.InfernalMobsCompat;

public class InfernalMobBonus implements IModifier {

	@Override
	public String getName() {
		return ChatColor.AQUA + MobHunting.getInstance().getMessages().getString("bonus.infernalmob.name");
	}

	@SuppressWarnings("unchecked")
	@Override
	public double getMultiplier(Entity entity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		double mul = 1;
		if (InfernalMobsCompat.isSupported()) {
			if (entity.hasMetadata(InfernalMobsCompat.MH_INFERNALMOBS)) {
				ArrayList<String> list = new ArrayList<>();
				if (entity.getMetadata(InfernalMobsCompat.MH_INFERNALMOBS).get(0).value() instanceof ArrayList<?>)
					list = (ArrayList<String>) entity.getMetadata(InfernalMobsCompat.MH_INFERNALMOBS).get(0).value();
				mul = Math.pow(MobHunting.getInstance().getConfigManager().multiplierPerInfernalLevel, list.size());
			}
		}
		return mul;
	}

	@Override
	public boolean doesApply(Entity entity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		return InfernalMobsCompat.isInfernalMob(entity);
	}
}

package metadev.digital.MetaMobHunting.compatibility;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import metadev.digital.MetaMobHunting.MobHunting;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.entity.EntityManager;

public class StackMobHelper {

	private static StackMob getStackMob() {
		return (StackMob) StackMobCompat.getPlugin();
	}
	
	public static EntityManager getEntityManager() {
		return new EntityManager(getStackMob());
	}

	public static boolean isStackedMob(LivingEntity entity) {
		if (StackMobCompat.isSupported()) {
			return getEntityManager().isStackedEntity(entity);
		}
		return false;
	}

	public static int getStackSize(LivingEntity entity) {
		return getEntityManager().getStackEntity(entity).getSize();
	}

	public static boolean killHoleStackOnDeath(Entity entity) {
		return getStackMob().getConfig().getBoolean("kill-all.enabled");
	}

	public static boolean isGrindingStackedMobsAllowed() {
		return MobHunting.getInstance().getConfigManager().isGrindingStackedMobsAllowed;
	}

}

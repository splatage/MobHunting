package one.lindegaard.MobHunting.compatibility;

import org.bukkit.entity.Entity;

import one.lindegaard.MobHunting.MobHunting;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.api.EntityManager;

public class StackMobHelper {

	private static StackMob getStackMob() {
		return (StackMob) StackMobCompat.getPlugin();
	}
	
	public static EntityManager getEntityManager() {
		return new EntityManager(getStackMob());
	}

	public static boolean isStackedMob(Entity entity) {
		if (StackMobCompat.isSupported()) {
			return getEntityManager().isStackedEntity(entity);
		}
		return false;
	}

	public static int getStackSize(Entity entity) {
		return getEntityManager().getStackedEntity(entity).getSize();
	}

	public static boolean killHoleStackOnDeath(Entity entity) {
		return getStackMob().getConfig().getBoolean("kill-all.enabled");
	}

	public static boolean isGrindingStackedMobsAllowed() {
		return MobHunting.getInstance().getConfigManager().isGrindingStackedMobsAllowed;
	}

}

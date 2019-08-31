package one.lindegaard.MobHunting.grinding;

import org.bukkit.entity.Entity;

import one.lindegaard.MobHunting.MobHunting;

public class GrindingInformation {

	private int entityId;
	private Entity killed;
	private Entity killer;
	private long timeOfDeath;
	private double cDampnerRange = MobHunting.getInstance().getConfigManager().grindingDetectionRange;

	GrindingInformation(Entity killer, Entity killed) {
		entityId = killed.getEntityId();
		this.killed = killed;
		this.killer = killer; 
		timeOfDeath=System.currentTimeMillis();
		cDampnerRange = MobHunting.getInstance().getConfigManager().grindingDetectionRange;
	}

	/**
	 * @return the entityId
	 */
	public int getEntityId() {
		return entityId;
	}

	/**
	 * @return the killed
	 */
	public Entity getKilled() {
		return killed;
	}

	/**
	 * @return the killer
	 */
	public Entity getKiller() {
		return killer;
	}

	/**
	 * @return the timeOfDeath
	 */
	public long getTimeOfDeath() {
		return timeOfDeath;
	}

	/**
	 * @param timeOfDeath the timeOfDeath to set
	 */
	public void setTimeOfDeath(long timeOfDeath) {
		this.timeOfDeath = timeOfDeath;
	}

	/**
	 * @return the cDampnerRange
	 */
	public double getcDampnerRange() {
		return cDampnerRange;
	}

	/**
	 * @param cDampnerRange the cDampnerRange to set
	 */
	public void setcDampnerRange(double cDampnerRange) {
		this.cDampnerRange = cDampnerRange;
	}

}

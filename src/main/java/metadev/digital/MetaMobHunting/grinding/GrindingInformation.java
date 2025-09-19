package metadev.digital.MetaMobHunting.grinding;

import java.lang.ref.WeakReference;
import java.util.UUID;

import org.bukkit.entity.Entity;

import metadev.digital.MetaMobHunting.MobHunting;

public class GrindingInformation {

	private int entityId;
	private WeakReference<Entity> killedRef;
	private UUID killer; //This is always a Player
	private long timeOfDeath;
	private double cDampnerRange = MobHunting.getInstance().getConfigManager().grindingDetectionRange;

	GrindingInformation(UUID killer, Entity killed) {
		entityId = killed.getEntityId();
		this.killedRef = new WeakReference<>(killed);
		this.killer = killer; 
		timeOfDeath = System.currentTimeMillis();
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
		return killedRef.get();
	}

	/**
	 * @return the killer
	 */
	public UUID getKillerUUID() {
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


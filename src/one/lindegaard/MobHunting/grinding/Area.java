package one.lindegaard.MobHunting.grinding;

import org.bukkit.Location;

public class Area {
	private Location center;
	private double range;
	private int count;
	private long time;

	public Area(Location location, double range, int count) {
		center = location;
		this.range = range;
		this.count = count;
		this.time = System.currentTimeMillis();
	}
	
	public Area(Location location, double range, int count, long time) {
		center = location;
		this.range = range;
		this.count = count;
		this.time = time;
	}

	/**
	 * @return the count
	 */
	public int getCount() {
		return count;
	}

	/**
	 * @param count the count to set
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * @return the time
	 */
	public long getTime() {
		return time;
	}

	/**
	 * @param time the time to set
	 */
	public void setTime(long time) {
		this.time = time;
	}

	/**
	 * @return location of the center of the Area
	 */
	public Location getCenter() {
		return center;
	}

	/**
	 * @param location
	 *            of the center of the Area
	 */
	public void setCenter(Location location) {
		this.center = location;
	}

	/**
	 * @return the range
	 */
	public double getRange() {
		return range;
	}

	/**
	 * @param range
	 *            the range to set
	 */
	public void setRange(double range) {
		this.range = range;
	}

	/**
	 * @return the count
	 */
	public int getCounter() {
		return count;
	}

	/**
	 * @param count
	 *            the count to set
	 */
	public void setCounter(int count) {
		this.count = count;
	}
}

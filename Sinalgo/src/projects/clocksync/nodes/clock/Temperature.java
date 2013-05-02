package projects.clocksync.nodes.clock;

public interface Temperature {
	/**
	 * 
	 * @param simTime
	 * @return the temperature of the clock in the time interval [simTime, simTime+1]. 
	 */
	public float getTemperature(long simTime);
}

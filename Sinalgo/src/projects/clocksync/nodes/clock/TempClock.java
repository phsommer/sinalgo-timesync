package projects.clocksync.nodes.clock;

import sinalgo.runtime.Global;

public abstract class TempClock extends RateClock {
	
	// drift parameters
	public static final float REF_TEMP = 24.0f;
	// assuming linear dependence of drift on temperature
	public static final float DRIFT_PER_DEGREE = -5E-7f;
	
	// private stuff
	protected static long ticksPerSecond(float temperature) {
		return Math.round(Clock.ticksPerSecond * (1+(temperature-REF_TEMP)*DRIFT_PER_DEGREE));
	}

	public TempClock (double hardwareClockDrift, long hardwareTimeOffset) {
		super(hardwareClockDrift, hardwareTimeOffset);
	}
	
	public TempClock (double hardwareClockDrift, long hardwareTimeOffset, long logicalOffset) {
		super(hardwareClockDrift, hardwareTimeOffset, logicalOffset);
	}
	
	/**
	 * 
	 * @param second
	 * @return the temperature of the clock in the time interval [second, second+1].
	 */
	public abstract float getTemperature(long second);
	
	public long getHardwareTime() {
		if (Global.currentTime == lastRequestTime) {
			return (long) hardwareTime;
		}
		long temp = (long) hardwareTime;
		hardwareTime -= (1.0 + hardwareClockDrift) * (lastRequestTime%1)
			* ticksPerSecond(getTemperature((long) lastRequestTime));
		for (long i = (long) lastRequestTime; i < (long) Global.currentTime; i++) {
			hardwareTime += (1.0 + hardwareClockDrift) * ticksPerSecond(getTemperature(i));
		}
		hardwareTime += (1.0 + hardwareClockDrift) * (Global.currentTime%1)
			* ticksPerSecond(getTemperature((long) Global.currentTime));
		lastRequestTime = Global.currentTime;
		updateLogicalTime(temp);
		return (long) hardwareTime;
	}
	
	@Override
	public double getHardwareDrift() {
		return super.getHardwareDrift() + DRIFT_PER_DEGREE 
			* (getTemperature((long) Global.currentTime) - REF_TEMP);
	}
}

package projects.clocksync.nodes.clock;

import sinalgo.runtime.Global;

public class RateClock implements Clock {

	// hardware clock parameters
	public final double hardwareClockDrift;
	
	// private stuff
	protected double logicalClockDriftCorrection;
	
	protected double lastRequestTime;
	protected double hardwareTime;
	protected double logicalTime;

	public RateClock (double hardwareClockDrift, long hardwareTimeOffset) {
		this.hardwareClockDrift = hardwareClockDrift;
		lastRequestTime = Global.currentTime;
		hardwareTime = hardwareTimeOffset;
		logicalTime = hardwareTimeOffset;
	}
	public RateClock (double hardwareClockDrift, long hardwareTimeOffset, long logicalOffset) {
		this.hardwareClockDrift = hardwareClockDrift;
		lastRequestTime = Global.currentTime;
		hardwareTime = hardwareTimeOffset;
		logicalTime = logicalOffset;
	}
	
	public long getHardwareTime() {
		if (Global.currentTime == lastRequestTime) {
			return (long)Math.floor(hardwareTime);
		}
		
		double temp = hardwareTime;
		hardwareTime += hardwareClockDrift * (Global.currentTime - lastRequestTime)* ticksPerSecond + (Global.currentTime - lastRequestTime)* ticksPerSecond;
		lastRequestTime = Global.currentTime;
		updateLogicalTime(hardwareTime - temp);
		return (long) Math.floor(hardwareTime);
	}
	
	protected void updateLogicalTime(double hardwareDiff) {
		logicalTime += logicalClockDriftCorrection*hardwareDiff + hardwareDiff;
	}
	
	public double getHardwareDrift() {
		return hardwareClockDrift;
	}
	
	public long getLogicalTime() {
		/*
		if (Global.currentTime > lastRequestTime) {
			// update hardware time first
			getHardwareTime();
		}*/
		return Math.round(logicalTime);
	}
	
	public void adjustLogicalClock(double correction) {
		logicalTime += correction;
	}
	
	public void setDriftCorrection(double driftCorrection) {
		// we need to proceed in time until now before assigning new driftCorrection
		getHardwareTime();
		logicalClockDriftCorrection = driftCorrection;
	}
	
	public double getDriftCorrection() {
		return logicalClockDriftCorrection;
	}
	
}

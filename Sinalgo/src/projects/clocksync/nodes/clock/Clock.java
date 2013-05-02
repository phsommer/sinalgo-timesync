package projects.clocksync.nodes.clock;

public interface Clock {

	
	public static final int ticksPerSecond =  1000000;
	
	/** Hardware clock methods **/
	public long getHardwareTime();
	
	public double getHardwareDrift();
	
	
	/** Logical clock methods **/
	public long getLogicalTime();
	public void adjustLogicalClock(double correction);
	
	public double getDriftCorrection();
	public void setDriftCorrection(double rate);
	
}

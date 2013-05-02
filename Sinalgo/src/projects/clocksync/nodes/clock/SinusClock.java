package projects.clocksync.nodes.clock;

public class SinusClock extends TempClock {
	
	public static final long dayHasSeconds = 86400;
	
	private final double shift = (Math.random()-.5)*5000;
	
	public SinusClock (double hardwareClockDrift, long hardwareTimeOffset) {
		super(hardwareClockDrift, hardwareTimeOffset);
	}
	
	public SinusClock (double hardwareClockDrift, long hardwareTimeOffset, long logicalOffset) {
		super(hardwareClockDrift, hardwareTimeOffset, logicalOffset);
	}
	
	public float getTemperature(long second) {
		
		return (float) (12 + 15 * Math.sin((2*Math.PI*(second+shift))/dayHasSeconds));
	}
	
}

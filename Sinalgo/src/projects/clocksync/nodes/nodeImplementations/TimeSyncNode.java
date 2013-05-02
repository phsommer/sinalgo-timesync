package projects.clocksync.nodes.nodeImplementations;

import projects.clocksync.CustomGlobal;
import projects.clocksync.nodes.clock.Clock;
import projects.clocksync.nodes.clock.RateClock;
import projects.clocksync.nodes.timers.PeriodicTimer;
import projects.clocksync.statistics.Probe;
import sinalgo.configuration.WrongConfigurationException;
import sinalgo.nodes.Node;
import sinalgo.runtime.Global;


public abstract class TimeSyncNode extends Node {

	/** hardware and software clock implementation **/
	public Clock clock;
	/** Hardware Timer **/
	protected PeriodicTimer timer = null;

	
	public void init() {
		
		// jitter is uniformly distributed
		clock = new RateClock((2*CustomGlobal.rng.nextDouble()-1)*CustomGlobal.PARAMETER_CLOCK_DRIFT,
		0); 
		
		//CustomGlobal.rng.nextInt(1000));
		
		
//		clock = new SinusClock((2*CustomGlobal.rng.nextDouble()-1)*CustomGlobal.PARAMETER_CLOCK_DRIFT,
//				(long) CustomGlobal.rng.nextInt(10000000));

//		clock = new JumpClock((2*CustomGlobal.rng.nextDouble()-1)*CustomGlobal.PARAMETER_CLOCK_DRIFT,
//				(long) CustomGlobal.rng.nextInt(10000000), 15000+1000*Math.random(),
//				1000 + 1000 * Math.random(), (float) (25 * Math.random()),
//				(float) (10 + 10 * Math.random()));
		
		//clock = new JumpClock((2*CustomGlobal.rng.nextDouble()-1)*CustomGlobal.PARAMETER_CLOCK_DRIFT,
		//				(long) CustomGlobal.rng.nextInt(10000000));
		
	}
	
	public void finish() {
		//System.out.println("Node " + ID + ": \t" + (1 + clock.getSoftwareRate())*(1 + clock.getHardwareRate()) + " SoftwareRate: " + clock.getSoftwareRate() + " HardwareRate: " + clock.getHardwareRate());
		
	}
	
	public Probe probe(int id) {
		
		// probe message		  
		Probe probe = new Probe(id, Global.currentTime, clock.getHardwareTime(), clock.getLogicalTime(), clock.getDriftCorrection(), 24.0f);
		return probe;
	}
	
	
	public abstract void sendBeacon();

	@Override
	public void checkRequirements() throws WrongConfigurationException {}

	@Override
	public void neighborhoodChange() {}

	@Override
	public void postStep() {}

	@Override
	public void preStep() {}


	
}

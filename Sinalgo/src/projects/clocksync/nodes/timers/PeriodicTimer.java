package projects.clocksync.nodes.timers;

import projects.clocksync.nodes.nodeImplementations.*;
import sinalgo.nodes.timers.*;
import sinalgo.nodes.*;
import sinalgo.runtime.Global;


public class PeriodicTimer extends Timer {
	
	Node node;
	
	double interval, delay;
	
	
	public PeriodicTimer(Node node, double interval, double delay) {
		
		this.node = node;
		this.interval = interval;
		this.delay = delay;
	}
	
	
	public void start() {
		this.startRelative(interval + delay, node);
	}
	
	@Override
	public void fire() {
		
		// send beacon message
		if (node instanceof TimeSyncNode) ((TimeSyncNode)node).sendBeacon();
		else if (node instanceof ProbingNode) ((ProbingNode)node).fired();
		
		
		if (Global.isRunning) {
			this.startRelative(interval, node); // recursive restart of the timer
		}
	}
}

package projects.clocksync.nodes.timers;

import projects.clocksync.nodes.nodeImplementations.*;
import sinalgo.nodes.timers.*;
import sinalgo.nodes.*;


public class SingleShotTimer extends Timer {
	
	Node node;
	double timeout;
	
	
	public SingleShotTimer(Node node, double timeout) {
		
		this.node = node;
		this.timeout = timeout;
	}
	
	
	public void start() {
		this.startRelative(timeout, node);
	}
	
	@Override
	public void fire() {
		
		// send beacon message
		if (node instanceof GradientTimeSyncProtocolNode) ((GradientTimeSyncProtocolNode)node).sendBeacon();
		else if (node instanceof ProbingNode) ((ProbingNode)node).fired();
		
	}
}

package projects.clocksync.nodes.messages;

import sinalgo.nodes.messages.Message;

public class AdaptiveTimeSynchMessage extends GradientTimeSyncMessage {
	
	public final double kappa;
	
	public AdaptiveTimeSynchMessage (int id, long hardware, long logical,
			 double rate, long maxLogical, double kappa) {
		super(id, hardware, logical, rate, maxLogical);
		this.kappa=kappa;
	}

	@Override
	public Message clone() {
		return this;
	}
}

package projects.clocksync.nodes.messages;

import sinalgo.nodes.messages.Message;

public class GradientTimeSyncMessage extends Message {

	 public final int id;
	 public final long hardware;
	 public final long logical;
	 public final double rate;
	 public final long maxLogical;
	 
	 public GradientTimeSyncMessage (int id, long hardware, long logical,
			 double rate, long maxLogical) {
		 this.id=id;
		 this.hardware=hardware;
		 this.logical=logical;
		 this.rate=rate;
		 this.maxLogical=maxLogical;
	 }

	@Override
	public Message clone() {
		return this;
	}
}

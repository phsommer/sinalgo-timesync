package projects.clocksync.nodes.messages;

import sinalgo.nodes.messages.Message;

public class FloodingTimeSyncMessage extends Message {

	 public int id = 0;
	 public int root = 0;
	 public int seqNum = 0;
	 public long hardware = 0;
	 public long offset = 0;
	 public double rate = 0;
	 
	@Override
	public Message clone() {	
		return this;
	};
}

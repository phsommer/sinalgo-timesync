package projects.clocksync.nodes.nodeImplementations;

import sinalgo.nodes.messages.*;
import projects.clocksync.*;
import projects.clocksync.nodes.messages.*;
import projects.clocksync.nodes.timers.*;

import java.util.*;


public class GradientTimeSyncProtocolNode extends TimeSyncNode {
	
	protected boolean jumped = false;
	
	/** synchronization error **/
	public long syncError = Long.MAX_VALUE;
	
	/** Estimate of maximum logical clock value in the network **/
	long diffMaxLogical = 0;

	// data structures for the timesync algorithm
	Map<Integer, NodeInfo> nodeTable = new HashMap<Integer, NodeInfo>();
	
	/**
	 * Represents a synchronization point
	 * @author psommer
	 *
	 */
	protected class NodeInfo {
		
		// not to be used by algorithm, just for convenient debugging!
		final TimeSyncNode node;
		
		final int id;
		final long msgNodeHardware;
		final long msgNodeLogical;
		final long msgNeighborHardware;
		final long msgNeighborLogical;
		final double neighborRate;
		final double relativeRate;
		
		public NodeInfo(TimeSyncNode node, int id, long msgNodeHardware, long msgNodeLogical,
				long msgNeighborHardware, long msgNeighborLogical, double neighborrate,
				double relativeRate) {
			this.node=node;
			this.id=id;
			this.msgNodeHardware=msgNodeHardware;
			this.msgNodeLogical=msgNodeLogical;
			this.msgNeighborHardware=msgNeighborHardware;
			this.msgNeighborLogical=msgNeighborLogical;
			this.neighborRate=neighborrate;
			this.relativeRate=relativeRate;
		}
	}
	
	
	
	
	@Override
	public void init() {
	
		super.init();
		
		// start periodic timer with the given BEACON_INTERVAL
		timer = new PeriodicTimer(this, CustomGlobal.PARAMETER_BEACON_INTERVAL,
				CustomGlobal.rng.nextDouble()
				* CustomGlobal.PARAMETER_BEACON_INTERVAL); // asynchronous
		timer.start();
		
	}
	
	
	/**
	 * ovverride if you want to process messages differently
	 */
	public void handleMessages(Inbox inbox) {
		
		while (inbox.hasNext()) {
			
			GradientTimeSyncMessage msg = (GradientTimeSyncMessage) inbox.next();
			
			// get previous sync point
			NodeInfo info = nodeTable.get(msg.id);
			
			double relativeRate = 0;
			if (info!=null) {
				// calculate length of synchronization interval in local time units
				long deltaLocal = clock.getHardwareTime() - info.msgNodeHardware;
				// calculate number of neighbors' ticks in synchronization interval
				long deltaNeighbor = Math.round((msg.hardware - info.msgNeighborHardware)
						+(msg.hardware - info.msgNeighborHardware)*msg.rate);
				
				//System.out.println("deltaLocal: " + deltaLocal + ", deltaNeighbor: " + deltaNeighbor);
				
				// calculate relative clock drift (hj*lj/hi)
				double currentRate = (double)(deltaNeighbor-deltaLocal)/deltaLocal;
				
				// update relative clock rate
				relativeRate = CustomGlobal.PARAMETER_MOVING_AVERAGE_ALPHA*info.relativeRate + (1-CustomGlobal.PARAMETER_MOVING_AVERAGE_ALPHA)*currentRate;
				
				// compare received estimate of max. clock value with own
				diffMaxLogical = Math.max(diffMaxLogical, msg.maxLogical-clock.getLogicalTime());
			}
			
			// calculate clock offset and jump if too large
			long offset = msg.logical - clock.getLogicalTime();
			
			if (Math.abs(offset)>Math.abs(syncError)) syncError = offset;
			
			if (offset>CustomGlobal.PARAMETER_JUMP_THRESHOLD) {
				// adjust offset (jump)
				clock.adjustLogicalClock(offset);
				diffMaxLogical = Math.max(diffMaxLogical-offset, 0);
				//System.out.println("Jumped!!");
				jumped = true;
			}
			
			info = new NodeInfo((TimeSyncNode) inbox.getSender(), msg.id, clock.getHardwareTime(),
					clock.getLogicalTime(), msg.hardware, msg.logical, msg.rate, relativeRate);
			
			// save node info
			nodeTable.put(msg.id, info);
		}
		
	}
	

	/**
	 * override in case you want to send different information
	 */
	@Override
	public void sendBeacon() {
		
		//call algorithm
		double avgRate = computeRate();
		//System.out.println(ID + " sets software rate: " + avgRate);
		clock.setDriftCorrection(avgRate);
		
		// respect constant fraction of delay in sent values (thus no handling by recipient!)
		// current handling neglects clock drift during message transfer (would be rounded to 0
		// anyway)
		GradientTimeSyncMessage msg = new GradientTimeSyncMessage(ID,
				clock.getHardwareTime()+CustomGlobal.PARAMETER_MESSAGE_DELAY_CONST,
				clock.getLogicalTime()+CustomGlobal.PARAMETER_MESSAGE_DELAY_CONST, avgRate,
				clock.getLogicalTime()+diffMaxLogical+CustomGlobal.PARAMETER_MESSAGE_DELAY_CONST);
		broadcast(msg);
		
		syncError = 0;
	}

	/**
	 * 
	 * @return the clock rate multiplier until the next beacon
	 */
	public double computeRate() {
		
				
		// else compute new average logical clock rate
		double avgRate = clock.getDriftCorrection();
		double avgOffset = 0;
		int neighborCount = 0;
		int offsetCount = 0;
		
		for (NodeInfo info : nodeTable.values()) {
			
			avgRate += info.relativeRate;
			neighborCount++;
			
			// estimate current offset
			
			
			long offset = info.msgNeighborLogical - info.msgNodeLogical;
			
			//System.out.println("\tOffset to node " + info.id + ": " + offset + " (reality is: " + (info.node.clock.getSoftwareTime() - nodeLogicalTime));
			
			//System.out.println("offset: " + offset);
			
			if (offset>0) {
				// neighbor is ahead in time
				avgOffset += 1*offset;
				offsetCount+=1;
			} else {
				
				if (Math.abs(offset)<CustomGlobal.PARAMETER_JUMP_THRESHOLD) {
					avgOffset += offset;
					offsetCount++;
				}
	
			}
			
									
		}
		
		// adjust logical clock offset
		if (offsetCount>0 && !jumped) {
			// adjust offset
			long correction = (long)Math.ceil(avgOffset / (offsetCount + 1));
			
			if (Math.abs(correction)<CustomGlobal.PARAMETER_JUMP_THRESHOLD) {
				// adjust clock offset
				clock.adjustLogicalClock(correction);
			}
			
		}
	
		jumped = false;
		// update and return clock Rate
		avgRate /= (neighborCount+1);
		return avgRate;
	}
}

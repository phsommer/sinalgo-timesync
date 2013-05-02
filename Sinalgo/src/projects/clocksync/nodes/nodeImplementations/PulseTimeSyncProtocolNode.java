package projects.clocksync.nodes.nodeImplementations;

import sinalgo.nodes.messages.*;
import projects.clocksync.*;
import projects.clocksync.nodes.messages.*;
import projects.clocksync.nodes.timers.*;

import java.io.*;
import java.util.*;




public class PulseTimeSyncProtocolNode extends TimeSyncNode {

	/** number of entries in the table **/
	public static int MAX_ENTRIES = 8;

	/** synchronization error **/
	public long syncError = Long.MAX_VALUE;
	
	/** linear regression table **/
	List<SyncTableEntry> regressionTable = new LinkedList<SyncTableEntry>();

	/** initialize sequence number to zero **/
	private int seqNum = 0;

	
	
	/**
	 * Represents a synchronization point
	 * @author psommer
	 *
	 */
	class SyncTableEntry {
		long localTime;
		long timeOffset;
	}
	
	
	static {
		// create output directory (if necessary)
		File dir = new File(CustomGlobal.path);
		if (!dir.exists()) dir.mkdirs();
	}
	
	
	
	@Override
	public void init() {
	
		super.init();
		
		if (ID==1) {
			
			// root node
			// start periodic timer with the given BEACON_INTERVAL
			timer = new PeriodicTimer(this, CustomGlobal.PARAMETER_BEACON_INTERVAL,
					CustomGlobal.rng.nextDouble()
					* CustomGlobal.PARAMETER_BEACON_INTERVAL); // asynchronous
			timer.start();
			
			//System.out.println("Starting root node");
		}
		
		
	}
	
	
	/**
	 * override if you want to process messages differently
	 */
	public void handleMessages(Inbox inbox) {
		
		while (inbox.hasNext()) {
			
			Message msg = inbox.next();
			
			// synchronization message
			FloodingTimeSyncMessage syncMsg = (FloodingTimeSyncMessage)msg;
			
			//System.out.println("@" + Global.currentTime + "Node: " + ID + " message received");
			//System.out.println("syncMsg.seqNum = " + syncMsg.seqNum + ", seqNum = " + seqNum);
			
			if (syncMsg.seqNum<seqNum) {
				// no new pulse, ignore the message
				return;
			}
			
			// get neighbors global time
			long neighborLogicalTime = syncMsg.hardware + syncMsg.offset ;
				
			// add entry to the linear regression table
			SyncTableEntry entry = new SyncTableEntry();
			entry.localTime = clock.getHardwareTime();
			entry.timeOffset = neighborLogicalTime - clock.getHardwareTime();
			
			// remove head if queue is already full
			if (regressionTable.size()==MAX_ENTRIES) {
				regressionTable.remove(0);
			}
			
			// add new entry to the regression table
			//System.out.println("Add message to the regression table");
			regressionTable.add(entry);
			calculateConversion();
			
			//System.out.println("Node " + ID + ", Table size: " + regressionTable.size());
			
			//System.out.println("Node " + ID + ", Add entry to the linear regression table.");
			
			//forward estimate; quick hack to forward the correct value
			long offset = neighborLogicalTime-clock.getLogicalTime();
			clock.adjustLogicalClock(offset);
			sendBeacon();
			clock.adjustLogicalClock(-offset);
		}
		
	}

	private void calculateConversion() {
		
		float skew = 0;
		
		//System.out.println("Size of regression table: " + regressionTable.size());
		
		if (regressionTable.isEmpty()) return;

	    long localAv = 0;
	    long offsetAv = 0;
	    
	    
	    for (SyncTableEntry entry : regressionTable) {
	        localAv += entry.localTime;
            offsetAv += entry.timeOffset;
	    }
	    
	    localAv = Math.round((double)localAv/regressionTable.size());
	    offsetAv = Math.round((double)offsetAv/regressionTable.size());
	    
	    
	    long localSum=0;
        long offsetSum=0;

        for (SyncTableEntry entry : regressionTable) {
	    
        	long a = entry.localTime - localAv;
	        long b = entry.timeOffset - offsetAv;

	        localSum += a * a;
	        offsetSum += a * b;
	    }

                
	    if( localSum != 0 ) {
	    	 skew = (float)offsetSum / (float)localSum;
	    	 clock.setDriftCorrection(skew);
	    	 //System.out.println("Set skew: " + skew);
	    }
	           
	    //System.out.println("new rate: " + clock.getSoftwareRate());
	    
	    SyncTableEntry tail = regressionTable.get(regressionTable.size()-1);
	    
	    long offsetEstimated = offsetAv + Math.round(skew*(tail.localTime - localAv));
	    
	    long offsetNow = clock.getLogicalTime() - clock.getHardwareTime();
	    
	    long offset = offsetEstimated - offsetNow;
	    
	    //System.out.println("Add offset: " + offset);
	    clock.adjustLogicalClock(offset);
	    

	}

	
	/**
	 * override in case you want to send different information
	 */
	@Override
	public void sendBeacon() {
		
		//System.out.println("Node " + ID + ", Timer fired @" + Global.currentTime);
		//System.out.println("RootId: " + rootId + ", heartBeats: " + heartBeats);
		
		// broadcast message
		FloodingTimeSyncMessage msg = new FloodingTimeSyncMessage();
		msg.id = ID;
		
		msg.hardware = clock.getHardwareTime() + CustomGlobal.PARAMETER_MESSAGE_DELAY_CONST;
		msg.offset = clock.getLogicalTime() + CustomGlobal.PARAMETER_MESSAGE_DELAY_CONST - msg.hardware;
		msg.seqNum = seqNum;
		
		this.broadcast(msg);
		
		// root increases sequence number
		seqNum++;
		
		//System.out.println("Send msg, node: " + ID + " my root is " + rootId);

		syncError = 0;
		
	}
	
	
	


}

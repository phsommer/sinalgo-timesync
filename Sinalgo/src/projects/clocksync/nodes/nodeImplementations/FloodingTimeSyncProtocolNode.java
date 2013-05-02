package projects.clocksync.nodes.nodeImplementations;

import sinalgo.nodes.messages.*;
import projects.clocksync.*;
import projects.clocksync.nodes.messages.*;
import projects.clocksync.nodes.timers.*;

import java.io.*;
import java.util.*;




public class FloodingTimeSyncProtocolNode extends TimeSyncNode {
	
	/** after becoming the root ignore other roots messages (in send period) **/
	private static final int IGNORE_ROOT_MSG = 0; // 4

	/** if time sync error is bigger than this clear the table **/
	private static final long ENTRY_THROWOUT_LIMIT = Long.MAX_VALUE; //500;

	/** number of entries in the table **/
	public static int MAX_ENTRIES = 8;

	/** number of entries to become synchronized **/
	private static final int ENTRY_VALID_LIMIT = 4;

	/** number of entries to send sync messages **/
	private static final int ENTRY_SEND_LIMIT = 3;
	
	/** number of rounds until to declare itself the new root **/
	private int ROOT_TIMEOUT = Integer.MAX_VALUE;

	/** synchronization error **/
	public long syncError = Long.MAX_VALUE;
	
	/** linear regression table **/
	List<SyncTableEntry> regressionTable = new LinkedList<SyncTableEntry>();
	
	/** initialize rootId with the current node ID **/
	private int rootId = 0;

	/** initialize sequence number to zero **/
	private int seqNum = 0;
	
	private int heartBeats = 0;

	private int numErrors = 0;

	
	
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
		
		rootId = Integer.MAX_VALUE;
		
		// TODO: this tweak guarantees that Node 1 will start to advertise itself as root node before all others
		if (ID==1) ROOT_TIMEOUT = 0;
		
		
		// start periodic timer with the given BEACON_INTERVAL
		timer = new PeriodicTimer(this, CustomGlobal.PARAMETER_BEACON_INTERVAL,
				CustomGlobal.rng.nextDouble()
				* CustomGlobal.PARAMETER_BEACON_INTERVAL); // asynchronous
		timer.start();
		
	}
	
	
	/**
	 * override if you want to process messages differently
	 */
	public void handleMessages(Inbox inbox) {
		
		while (inbox.hasNext()) {
			
			Message msg = inbox.next();
			
			// synchronization message
			FloodingTimeSyncMessage syncMsg = (FloodingTimeSyncMessage)msg;
			
			//System.out.println("@" + Global.currentTime + "Node: " + ID + " from " + senderId);
			//System.out.println("syncMsg.seqNum = " + syncMsg.seqNum + ", seqNum = " + seqNum);
			
			if (syncMsg.root<rootId && !(heartBeats < IGNORE_ROOT_MSG && rootId == ID)) {
				// new reference node elected
				System.out.println("Node " + ID + ", Changing reference node to: " + syncMsg.root);	
				rootId = syncMsg.root;
				seqNum  = syncMsg.seqNum;
					
			} else if (rootId==syncMsg.root && syncMsg.seqNum>seqNum) {
				seqNum = syncMsg.seqNum;
			} else {
				// ignore this message
				//System.out.println("Message ignored:");
				continue;
			}
				
			
			if (rootId < ID) {
				heartBeats = 0;
			}
			
			// add random noise to the timestamp
			long nodeHardwareTime = clock.getHardwareTime();
			// convert to global time
			long nodeLogicalTime = clock.getLogicalTime();
			
			// get neighbors global time
			long neighborLogicalTime = syncMsg.hardware + syncMsg.offset;
				
			// add entry to the linear regression table
			
			long timeErrorAbs = Math.abs(neighborLogicalTime - nodeLogicalTime);
			if (isSynced() && timeErrorAbs>ENTRY_THROWOUT_LIMIT) {
				if (++numErrors>3) clearTable();
				continue; // don't incorporate a bad reading
			}
			
			numErrors = 0;
			
			SyncTableEntry entry = new SyncTableEntry();
			entry.localTime = nodeHardwareTime;
			entry.timeOffset = neighborLogicalTime - nodeHardwareTime;
			
			// remove head if queue is already full
			if (regressionTable.size()==MAX_ENTRIES) regressionTable.remove(0);
			
			// add new entry to the regression table
			//System.out.println("Add message to the regression table");
			regressionTable.add(entry);
			calculateConversion();
			
			//System.out.println("Node " + ID + ", Add entry to the linear regression table.");
		
		
		}
		
	}
	
	private boolean isSynced() {
		return (regressionTable.size()>=ENTRY_VALID_LIMIT || rootId==ID);
	}


	private void clearTable() {
		System.out.println("Node " + ID + ": Clear table");
		regressionTable.clear();
	}

	private void calculateConversion() {
		
		float skew = 0;
		
		//System.out.println("Size of regression table: " + regressionTable.size());
		
		if (regressionTable.isEmpty()) return;
		
		SyncTableEntry head = regressionTable.get(0);
		
		/*
        We use a rough approximation first to avoid time overflow errors. The idea
        is that all times in the table should be relatively close to each other.
		*/
	    long newLocalAverage = head.localTime;
	    long newOffsetAverage = head.timeOffset;

	    long localSum = 0;
	    long localAverageRest = 0;
	    long offsetSum = 0;
	    long offsetAverageRest = 0;
	    int tableEntries = regressionTable.size();
	    
	    for (SyncTableEntry entry : regressionTable) {
	        localSum += (entry.localTime - newLocalAverage) / tableEntries;
	        localAverageRest += (entry.localTime - newLocalAverage) % tableEntries;
            offsetSum += (entry.timeOffset - newOffsetAverage) /  tableEntries;
            offsetAverageRest += (entry.timeOffset - newOffsetAverage) % tableEntries;
	    }
	    
	    newLocalAverage += localSum + localAverageRest / tableEntries;
        newOffsetAverage += offsetSum + offsetAverageRest / tableEntries;
	    

        for (SyncTableEntry entry : regressionTable) {
	    
        	long a = entry.localTime - newLocalAverage;
	        long b = entry.timeOffset - newOffsetAverage;

	        localSum += a * a;
	        offsetSum += a * b;
	    }

                
	    if( localSum != 0 ) {
	    	 skew = (float)offsetSum / (float)localSum;
	    	 clock.setDriftCorrection(skew);
	    	 //System.out.println("Set skew: " + skew);
	    }
	           
	    
	    SyncTableEntry tail = regressionTable.get(regressionTable.size()-1);
	    long localDiff = tail.localTime - newLocalAverage;
	    //System.out.println("localDiff: " + localDiff);
	    
	    long offsetDiff = Math.round(skew*localDiff);
	    //System.out.println("offsetDiff: " + offsetDiff);
	    
	    long offsetEstimated = newOffsetAverage + offsetDiff;
	    
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
		//System.out.println("RootId: " + rootId + ", heartBeats: " + heartBeats + ", ROOT_TIMEOUT: " + ROOT_TIMEOUT);
		
		if (rootId == Integer.MAX_VALUE && ++heartBeats >= ROOT_TIMEOUT ) {
			seqNum = 0;
			rootId = ID;
			System.out.println("Node " + ID + " declares itself as the new root");
		}
		
		
		if (rootId != Integer.MAX_VALUE) {
			
			if (rootId != ID && heartBeats>=ROOT_TIMEOUT) {
				// root timeout
				heartBeats = 0;
				rootId = ID;
				seqNum++; 
			}
			
			if (rootId !=ID && regressionTable.size()<ENTRY_SEND_LIMIT) {
				// we don't send time sync msg, if we don't have enough data
				return;
			}
			
			// broadcast message
			FloodingTimeSyncMessage msg = new FloodingTimeSyncMessage();
			msg.id = ID;
			
			
			// get hardware and logical time (corrected by the transmission delay)
			long nodeHardwareTime = clock.getHardwareTime() + CustomGlobal.PARAMETER_MESSAGE_DELAY_CONST;
			long nodeLogicalTime = clock.getLogicalTime() + CustomGlobal.PARAMETER_MESSAGE_DELAY_CONST;
			
			msg.hardware = nodeHardwareTime;
			msg.offset = nodeLogicalTime - nodeHardwareTime;
			msg.rate = clock.getDriftCorrection();
			msg.root = rootId;
			msg.seqNum = seqNum;
			
			this.broadcast(msg);
			
			// root increases sequence number
			if (rootId == ID) seqNum++;
			
			//System.out.println("Send msg, node: " + ID + " my root is " + rootId);
			syncError = 0;
			
			
		} else {
			
			// do nothing and wait until next round
			
		}
		
	}
	
	
	


}

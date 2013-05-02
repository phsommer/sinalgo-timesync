package projects.clocksync.statistics;

import java.util.*;
import java.io.*;

import org.apache.commons.math.stat.descriptive.*;

import sinalgo.nodes.*;



public class Statistics {

	
	static String path = null;
	static boolean batch = false;

	static SummaryStatistics neighborError = new SummaryStatistics();
	static SummaryStatistics networkError = new SummaryStatistics();
	private static int PROBE_THRESHOLD = 500;
	private static int PROBE_NUMBER = 1000;
	private static Node rootNode = null;
	static Map<Node, SummaryStatistics> nodeErrorMap = new HashMap<Node, SummaryStatistics>();

	
	public Statistics(String path, boolean batch, List<Node> nodes) {

		File dir = new File(path);
		dir.mkdirs();
				
		Statistics.path = path;
		Statistics.batch = batch;
		
		neighborError.clear();
		networkError.clear();
		
		nodeErrorMap.clear();
		
		for (Node node : nodes) {
			if (node!=rootNode)	nodeErrorMap.put(node, new SummaryStatistics());			
		}
				
	}
	
	
	public static SummaryStatistics getNeighborError() {
		return neighborError;
	}
	
	public static SummaryStatistics getNetworkError() {
		return networkError;
	}
	
	public static Map<Node, SummaryStatistics> getHopError() {
		return nodeErrorMap;
	}
	
	
	
		
	public void nextProbe(int id, List<Node> nodes, Map<Node,Probe> results) {
		
		long maxNeighbor = 0, maxNetwork = 0;
		
		//System.out.println("Root node: " + rootNode.ID);
		
		for (Node node : nodes) {
			
			//System.out.println("Node: " + node.ID);
			
			Probe probe = results.get(node);
			
			if (probe!=null) {
				
				for (Node other: nodes) {
					
					
					if (node!=other && node.ID<other.ID) {
						
						//System.out.println("\tNode: " + other.ID);
						
						Probe otherProbe = null;
						if ((otherProbe = results.get(other))!=null) {							
				
							
							long error = Math.abs(getSynchronizationError(probe.logical, otherProbe.logical));
							
							//if (error>1000) System.out.println("Node: " + node.ID + " <-> " + other.ID + " = " + error);
							
							// update network error
							maxNetwork = Math.max(maxNetwork, error);
							
							if (node.hasConnectionTo(other)) {
								// update neighbor error
								maxNeighbor = Math.max(maxNeighbor, error);
							}
							
							
							// calculate per node error
							if (node==rootNode) {
								SummaryStatistics nodeError = nodeErrorMap.get(other);
								if (nodeError!=null) {
									//System.out.println("Node: " + node.ID + ", error: " + error);
									nodeError.addValue(error);
								}
							}

						}

					}

				}

			} // for neighbors
								
			
		}// for nodes
		
		
		// update stats
		
		networkError.addValue(maxNetwork);
		neighborError.addValue(maxNeighbor);
		
	
	}
	

	public void finish() {

	}
	
	
	private static long getSynchronizationError(long a, long b) {
	
		return Math.abs(a - b);
		
 		/*
		if (error >= 0xEFFFFFFL) {
		  error = 0xFFFFFFFFL - error;
		}
		
		if (a>b) return error;
		else return -error;
		*/
	}


	public static void setProbeThreshold(int i) {
		PROBE_THRESHOLD  = i;
	}

	public static int getProbeThreshold() {
		return PROBE_THRESHOLD;
	}

	public static void setProbes(int i) {
		PROBE_NUMBER = i;
	}
	
	public static int getProbes() {
		return PROBE_NUMBER;
	}
	
	public static void setRoot(Node node) {
		rootNode  = node;
	}
	
	private int getHopCount(Node node) {
		// TODO: make this more general
		// get the hop count for a list topology (root = 1)
		return node.ID - 1;
	}
	
}

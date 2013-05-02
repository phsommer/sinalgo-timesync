package projects.clocksync.nodes.nodeImplementations;

import sinalgo.configuration.WrongConfigurationException;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.*;
import sinalgo.runtime.*;
import sinalgo.runtime.Runtime;

import projects.clocksync.*;
import projects.clocksync.nodes.timers.*;
import projects.clocksync.statistics.Probe;
import projects.clocksync.statistics.Statistics;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;

public class ProbingNode extends Node {

	private Connection con = null;
	private Statement stmt = null;
	private static final String DATABASE_FILE = "data.sqlite.db";
	private PreparedStatement pstmt1 = null, pstmt2 = null;

	
	SingleShotTimer timer = null;
	
	final static int PARAM_PROBING_INTERVAL_MEAN = 20;
	final static double PARAM_PROBING_INTERVAL_VAR = 5.0;
	private int probeId = 0;

	Map<Node, Probe> results = new HashMap<Node, Probe>();
	List<Node> nodes = new LinkedList<Node>();
	
	Statistics stats = null;
	
	private static Object monitor = new Object();
	
	
	@Override
	public void checkRequirements() throws WrongConfigurationException {
	}

	@Override
	public void handleMessages(Inbox inbox) {
		
	}

	@Override
	public void init() {
		
		
		// connect to the database
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (Exception e) {
			System.err.println("Logging: " + e.getMessage());
		}

		String url = "";
		File dir = new File(CustomGlobal.path);
		dir.mkdirs();
		
		url= "jdbc:sqlite:" + CustomGlobal.path + "/" + DATABASE_FILE;
		
		try {
			
			con = DriverManager.getConnection(url);
			
			// create databases
			stmt = con.createStatement();
			
			stmt.execute("DROP TABLE IF EXISTS replies");
			stmt.execute("CREATE TABLE replies (node integer, probe integer, source integer, time double, localtime integer, globaltime integer, offset integer, skew double, rssi integer, lqi integer, prr integer, root integer, sync integer, temperature double);");
			
			stmt.execute("DROP TABLE IF EXISTS probes");
			stmt.execute("CREATE TABLE probes (source integer, probe integer, time double);");
			
			stmt.execute("DROP TABLE IF EXISTS links");
			stmt.execute("CREATE TABLE links (node integer, seq integer, neighbor integer, inquality integer, outquality integer, time double);");
			
			stmt.execute("DROP TABLE IF EXISTS timesync");
			stmt.execute("CREATE TABLE timesync (node integer, seq integer, offset integer, rate double, localtime integer, time double);");
			
			// prepare statements
			pstmt1  = con.prepareStatement("INSERT INTO probes (source, probe, time) VALUES (?, ?, ?);");
			pstmt2 = con.prepareStatement("INSERT INTO replies (node, probe, source, time, localtime, globaltime, skew, rssi, lqi, root, sync, temperature) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
			
			// disable auto-commit
			con.setAutoCommit(false);
			
			
		} catch (Exception e) {
			System.err.println("Experiment: " + e.getMessage());
		}
		
		
		for (Node node: sinalgo.runtime.Runtime.nodes) {
			if (node instanceof TimeSyncNode) {
				nodes.add(node);
			}
		}
		
		stats = new Statistics("/home/psommer/measurements/Sinalgo/", true, nodes);
		
		
		timer = new SingleShotTimer(this, (int)Math.round(PARAM_PROBING_INTERVAL_MEAN + PARAM_PROBING_INTERVAL_VAR*CustomGlobal.rng.nextDouble()));
		timer.start();
		
	}

	@Override
	public void neighborhoodChange() {}

	@Override
	public void postStep() {}

	@Override
	public void preStep() {}
	
	
	public void fired() {
			
		//System.out.println("Probing time");
		
		probeId++;
		
		int maxProbes = Statistics.getProbes();
		
		if (!CustomGlobal.PARAMETER_BATCH_MODE && maxProbes/100!= 0 && probeId%(maxProbes/100)==0) {
			System.out.println((probeId/(maxProbes/100)) + "% finished.");
		}
		
		if (probeId==maxProbes) {
			
			// simulation is finished
			
			
			for (Node node : nodes) {
				
				TimeSyncNode timeSyncNode = (TimeSyncNode)node;
				timeSyncNode.finish();
			}
		
			// absolute logical rate difference
			
			double minRate = Double.POSITIVE_INFINITY;
			double maxRate = Double.NEGATIVE_INFINITY;
			
			for (Node node : nodes) {
				
				TimeSyncNode timeSyncNode = (TimeSyncNode)node;
				double absoluteRate = (1.0 + timeSyncNode.clock.getHardwareDrift())*(1.0 + timeSyncNode.clock.getDriftCorrection());
				minRate = Math.min(minRate, absoluteRate);
				maxRate = Math.max(maxRate, absoluteRate);
			}
			
			double diff = maxRate - minRate;
			
			System.out.println("RESULT_ABSOLUTE_RATE_DIFF " + diff);

			stats.finish();
			
			// clean-up
			try {
				
				if (stmt!=null) stmt.close();
				if (con!=null) con.close();
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		
			if (!CustomGlobal.PARAMETER_BATCH_MODE) {
				
				// get results
				SummaryStatistics neighborError = Statistics.getNeighborError();
				SummaryStatistics networkError = Statistics.getNetworkError();
				
				System.out.println("=====================================");
				System.out.println("Neighbor Synchronization Error:");
				System.out.println("\tAVG: " + neighborError.getMean());
				System.out.println("\tMIN: " + neighborError.getMin());
				System.out.println("\tMAX: " + neighborError.getMax());
				
				System.out.println("Network Synchronization Error:");
				System.out.println("\tAVG: " + networkError.getMean());
				System.out.println("\tMIN: " + networkError.getMin());
				System.out.println("\tMAX: " + networkError.getMax());
				System.out.println("=====================================");
				
				System.out.println("Per Node Synchronization Error:");
				
				Map<Node, SummaryStatistics> hopError = Statistics.getHopError();
				
				for (Entry<Node, SummaryStatistics> entry : hopError.entrySet()) {
					System.out.println("Node " + entry.getKey().ID + ":\t" + entry.getValue().getMean());
				}
				
			}
			
			
			// stop simulation by removing all pending events
			
			Runtime.removeAllAsynchronousEvents();
			Runtime.clearAllNodes();
			Global.customGlobal.onExit(); // may perform some cleanup ops
		
			synchronized(monitor) {
				monitor.notify();
			}
			
			
			return;
		}
		
		
		
		if (probeId>=Statistics.getProbeThreshold()) {
		
			//System.out.println("Take sample #" + probeId + " @" + Global.currentTime);
			
			if (CustomGlobal.PARAMETER_WRITE_LOGFILES) {
				
				try {
					// set source
					pstmt1.setInt(1, 0xFFFF);
					// set probe
					pstmt1.setInt(2, probeId);
					// set time
					pstmt1.setDouble(3, 1000.0*Global.currentTime);
					
					pstmt1.execute();
					
				} catch (Exception e) {
					System.err.println("Insert probe failed: " + e.getMessage());
				}
				
			}
			
			results.clear();
			
			for (Node node : nodes) {
				
				TimeSyncNode timeSyncNode = (TimeSyncNode)node;
				Probe probe = timeSyncNode.probe(probeId);
				
				//System.out.println("Probing node " + node.ID + " time: " + probe.logical);
				
				// handle reply
				if (CustomGlobal.PARAMETER_WRITE_LOGFILES) {
					try {
						pstmt2.setInt(1, node.ID);
						pstmt2.setInt(2, probe.id);
						pstmt2.setInt(3, 0xFFFF);
						pstmt2.setDouble(4, 1000.0*Global.currentTime);
						pstmt2.setLong(5, probe.hardware);
						pstmt2.setLong(6, probe.logical);
						pstmt2.setDouble(7, probe.rate);
						pstmt2.setInt(8, 0); //rssi
						pstmt2.setInt(9, 0); //lqi
						pstmt2.setInt(10, 0); // TODO: root
						pstmt2.setBoolean(11, false); // sync
						pstmt2.setDouble(12, probe.temperature);
						
						pstmt2.execute();
						
					} catch (Exception e) {
						System.out.println("rate: " + probe.rate + ", temperature: " + probe.temperature);
						System.err.println("Insert reply failed: " + e.getMessage());
					}
					
					// commit transaction
					try {
						con.commit();
					} catch (SQLException e) {
						e.printStackTrace();
					}
					
				}
				

				results.put(node, probe);
				
			}
			
			// calculate stats
			stats.nextProbe(probeId, nodes, results);
			
		}
		
		// start timer
		if (Global.isRunning) {
			timer = new SingleShotTimer(this, (int)Math.round(PARAM_PROBING_INTERVAL_MEAN + PARAM_PROBING_INTERVAL_VAR*CustomGlobal.rng.nextDouble()));
			timer.start();
		}
		
	}
	
	public static void waitForCompletion() {
		synchronized(monitor) {
			try {
				monitor.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	

}

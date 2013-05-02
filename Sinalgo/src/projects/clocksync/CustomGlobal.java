/*
 Copyright (c) 2007, Distributed Computing Group (DCG)
                    ETH Zurich
                    Switzerland
                    dcg.ethz.ch

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 - Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the
   distribution.

 - Neither the name 'Sinalgo' nor the names of its contributors may be
   used to endorse or promote products derived from this software
   without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package projects.clocksync;



import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import sinalgo.nodes.Node;
import sinalgo.runtime.AbstractCustomGlobal;
import sinalgo.runtime.Global;
import sinalgo.tools.Tools;
import sinalgo.tools.statistics.Distribution;
import sinalgo.tools.statistics.UniformDistribution;

import projects.clocksync.nodes.Topology;
import projects.clocksync.nodes.clock.Clock;
import projects.clocksync.nodes.nodeImplementations.*;
import projects.clocksync.statistics.Statistics;


/**
 * This class holds customized global state and methods for the framework. 
 * The only mandatory method to overwrite is 
 * <code>hasTerminated</code>
 * <br>
 * Optional methods to override are
 * <ul>
 * <li><code>customPaint</code></li>
 * <li><code>handleEmptyEventQueue</code></li>
 * <li><code>onExit</code></li>
 * <li><code>preRun</code></li>
 * <li><code>preRound</code></li>
 * <li><code>postRound</code></li>
 * <li><code>checkProjectRequirements</code></li>
 * </ul>
 * @see sinalgo.runtime.AbstractCustomGlobal for more details.
 * <br>
 * In addition, this class also provides the possibility to extend the framework with
 * custom methods that can be called either through the menu or via a button that is
 * added to the GUI. 
 */
public class CustomGlobal extends AbstractCustomGlobal 
{
	
	private static final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy HH:mm");
	
		
	// clock parameters
	public static double PARAMETER_CLOCK_DRIFT = 30E-6f;
	
	// message delay
	public static long PARAMETER_MESSAGE_DELAY_CONST = 1500;
	public static double PARAMETER_MESSAGE_DELAY_JITTER = 1;
	
	// network topology
	public static String PARAMETER_TOPOLOGY = "CLIQUE";
	public static boolean PARAMETER_SHUFFLE_NODE_POSITION = false;
	public static int PARAMETER_NUMBER_OF_NODES = 20;
	public static Class<? extends TimeSyncNode> PARAMETER_NODE_TYPE
		= PulseTimeSyncProtocolNode.class;
	
	// simulation-specific parameters
	public static String PARAMETER_SIMULATION_LABEL = PARAMETER_NODE_TYPE.getSimpleName() + " @" + format.format(new Date());
	public static boolean PARAMETER_WRITE_LOGFILES = false;
	
	// algorithm-specific parameters
	public static int PARAMETER_BEACON_INTERVAL = 30;
	public static double PARAMETER_MOVING_AVERAGE_ALPHA = 0.90f;
	public static int PARAMETER_JUMP_THRESHOLD = 10;
	
	public static boolean PARAMETER_BATCH_MODE = false;
	
	// path
	public static String path = System.getProperty("user.home")
	+ "/measurements/Sinalgo/" + PARAMETER_NODE_TYPE.toString().substring(
			PARAMETER_NODE_TYPE.toString().lastIndexOf(".")+1);

	
	public static Random rng = null;

	// distribution of the message delay
	public static Distribution MessageDelayDistribution = new UniformDistribution((double)(CustomGlobal.PARAMETER_MESSAGE_DELAY_CONST-CustomGlobal.PARAMETER_MESSAGE_DELAY_JITTER/2.0)/Clock.ticksPerSecond,
			(double)(CustomGlobal.PARAMETER_MESSAGE_DELAY_CONST + CustomGlobal.PARAMETER_MESSAGE_DELAY_JITTER/2.0)/Clock.ticksPerSecond);
	
		
	/* (non-Javadoc)
	 * @see runtime.AbstractCustomGlobal#hasTerminated()  
	 */
	public boolean hasTerminated() {
		return false;
	}

	
	@Override
	public void preRun() {
	
		
	// reset random number generator
	rng = Distribution.getRandom();
		
		
	// create network topology
        List<Node> nodes = null;
        
        
        if (PARAMETER_TOPOLOGY.equals("CLIQUE")) {
        	nodes = Topology.createClique(PARAMETER_NODE_TYPE, PARAMETER_NUMBER_OF_NODES);
        }
        else if (PARAMETER_TOPOLOGY.equals("RING")) {
        	nodes = Topology.createRing(PARAMETER_NODE_TYPE, PARAMETER_NUMBER_OF_NODES, PARAMETER_SHUFFLE_NODE_POSITION);
        }
        else if (PARAMETER_TOPOLOGY.equals("LIST")) {
        	nodes = Topology.createList(PARAMETER_NODE_TYPE, PARAMETER_NUMBER_OF_NODES, PARAMETER_SHUFFLE_NODE_POSITION);
        }
        else if (PARAMETER_TOPOLOGY.equals("GRID")) {
        	
        	int width = (int)Math.floor(Math.sqrt(PARAMETER_NUMBER_OF_NODES));
        	int height = PARAMETER_NUMBER_OF_NODES / width;
	       	nodes = Topology.createGrid(PARAMETER_NODE_TYPE, width, height, PARAMETER_SHUFFLE_NODE_POSITION);
        } else if (PARAMETER_TOPOLOGY.equals("RANDOM")) {
        	nodes = Topology.createUDG(PARAMETER_NODE_TYPE, PARAMETER_NUMBER_OF_NODES, 10.0);
        	
        }
       
        
        // set root node
        
        for (Node node : nodes) {
        	if (node.ID==1) {
        		Statistics.setRoot(node);
        		break;
        	}
        }
        
        
        File path = new File(CustomGlobal.path);
        if (!path.exists()) {
        	System.err.println("Path does not exist: " + path);
        	System.exit(0);
        }
        
        if (!CustomGlobal.PARAMETER_BATCH_MODE) System.out.println("Path is: " + path);
        
		initializeNodes(nodes);
		
		System.out.println("Random number generator SEED = " + Distribution.getSeed());
		
	}
	
	
	
	private void initializeNodes(List<Node> nodes) {
		
		
				
		// probing node
	    ProbingNode prober = new ProbingNode();
	    
		for (Node node : nodes) {
			prober.addConnectionTo((TimeSyncNode)node);
			((TimeSyncNode)node).finishInitializationWithDefaultModels(true);
			
		}
		
		prober.finishInitializationWithDefaultModels(true);
		
	   
	    // Repaint the GUI as we have added some nodes
	 	if (Global.isGuiMode) Tools.repaintGUI();
	 	
	}
	

	public void onExit() {}

}

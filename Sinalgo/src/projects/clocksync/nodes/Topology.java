/* Copyright (c) 2008, Distributed Computing Group (DCG), ETH Zurich.
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. Neither the name of the copyright holders nor the names of
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS `AS IS'
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS
 *  BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, LOSS OF USE, DATA,
 *  OR PROFITS) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 *  THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  @author Philipp Sommer <sommer@tik.ee.ethz.ch>
 * 
 */

package projects.clocksync.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import sinalgo.nodes.Node;
import sinalgo.tools.statistics.Distribution;


public class Topology {

	
	final static int WIDTH = 1000;
	final static int HEIGHT = 1000;
	
    static long SEED = Distribution.getSeed();

    public static List<Node> createRing(Class<? extends Node> type, int nodes, boolean random) {

        // determine dimensions
        int CENTER_X = WIDTH / 2;
        int CENTER_Y = HEIGHT / 2;
        int RADIUS = 0;
        if (WIDTH > HEIGHT)
            RADIUS = HEIGHT / 2;
        else
            RADIUS = WIDTH / 2;

        // define graphical network topology

        double angle = 0;
        List<Node> list = new LinkedList<Node>();

        for (int i = 1; i <= nodes; i++) {
            try {
                Node node = type.newInstance();
                node.ID = i;
                list.add(node);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        // shuffle nodes
        if (random)
            Collections.shuffle(list, new Random(SEED));

        Node first = null;
        Node previous = null;

        for (Node node : list) {

            if (previous != null) {
                previous.addConnectionTo(node);
                node.addConnectionTo(previous);
            }

            previous = node;
            if (first == null)
                first = node;

            // set node position
            node.setPosition((int) Math.round(RADIUS * Math.sin(angle) + CENTER_X), (int) Math.round(RADIUS * Math.cos(angle)) + CENTER_Y, 0);
            angle += 2 * Math.PI / nodes;

        }

        // close ring
        if (first != previous) {
            first.addConnectionTo(previous);
            previous.addConnectionTo(first);
        }

        return list;

    }

    public static List<Node> createGrid(Class<? extends Node> type, int nodesX, int nodesY, boolean random) {


        // determine dimensions
        int SPACE_X = WIDTH / (nodesX - 1);
        int SPACE_Y = HEIGHT / (nodesY - 1);

        int nodes = nodesX * nodesY;

        List<Node> list = new ArrayList<Node>();

        for (int i = 0; i < nodes; i++) {
            try {
                Node node = type.newInstance();
                node.ID = i + 1;
                list.add(node);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // shuffle nodes
        if (random)
            Collections.shuffle(list, new Random(SEED));

        int pos = 0;
        for (int col = 0; col < nodesX; col++) {
            for (int row = 0; row < nodesY; row++) {
                Node node = list.get(pos);
                node.setPosition(col * SPACE_X, row * SPACE_Y, 0);

                // connect to top neighbor
                if (row > 0) {
                    Node top = list.get(pos - 1);
                    if (top != null)
                        node.addBidirectionalConnectionTo(top);
                }

                // connect to left neighbor
                if (col > 0) {
                    Node left = list.get(pos - nodesY);
                    if (left != null)
                        node.addBidirectionalConnectionTo(left);
                }

                pos++;
            }
        }

        return list;
    }

    public static List<Node> createClique(Class<? extends Node> type, int nodes) {


        // determine dimensions
        int CENTER_X = WIDTH / 2;
        int CENTER_Y = HEIGHT / 2;
        int RADIUS = 0;
        if (WIDTH > HEIGHT)
            RADIUS = HEIGHT / 2;
        else
            RADIUS = WIDTH / 2;

        double angle = 0;

        List<Node> list = new LinkedList<Node>();

        for (int i = 1; i <= nodes; i++) {

            try {
                Node node = type.newInstance();
                node.ID = i;
                node.setPosition(RADIUS * Math.sin(angle) + CENTER_X, RADIUS * Math.cos(angle) + CENTER_Y, 0);
                list.add(node);

            } catch (Exception e) {
                e.printStackTrace();
            }

            angle += 2 * Math.PI / nodes;

        }

        // add neighborhood relations

        for (Node node : list) {
            for (Node neighbor : list) {
                if (node != neighbor)
                    node.addConnectionTo(neighbor);
            }
        }

        return list;
    }

    

    public static List<Node> createGraph(Node type, int nodes, double p) {

        // determine dimensions
        int CENTER_X = WIDTH / 2;
        int CENTER_Y = HEIGHT / 2;

        LinkedList<Node> list = new LinkedList<Node>();

        Random rng = new Random();

        for (int i = 1; i <= nodes; i++) {

            try {
                Node node = type.getClass().newInstance();
                node.ID = i;
                node.setPosition(CENTER_X + rng.nextInt(WIDTH / 2), CENTER_Y + rng.nextInt(HEIGHT / 2), 0);
                list.add(node);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        for (Node a : list) {

            for (Node b : list) {

                if (a.ID < b.ID) {

                    if (rng.nextDouble() < p) {
                        // connect edge
                        a.addConnectionTo(b);
                        b.addConnectionTo(a);
                    }

                }

            }

        }

        return list;

    }
    
    
    public static List<Node> createUDG(Class<? extends Node> type, int nodes, double maxDistance) {



        LinkedList<Node> list = new LinkedList<Node>();

        Random rng = new Random();

        for (int i = 1; i <= nodes; i++) {

            try {
                Node node = type.newInstance();
                node.ID = i;
                node.setPosition(WIDTH*rng.nextDouble(), HEIGHT*rng.nextDouble(), 0);
                list.add(node);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        for (Node a : list) {

            for (Node b : list) {

            	// calculate node distance
            	
            	double distance = a.getPosition().distanceTo(b.getPosition());
            	
                if (distance <= maxDistance) {
                	a.addConnectionTo(b);
                    b.addConnectionTo(a);
                    System.out.println("Added connection: " + a + " -> " + b);
                }

            }

        }

        return list;

    }
    

    public static List<Node> createList(Class<? extends Node> type, int nodes, boolean random) {


        // determine dimensions
        int CENTER_X = WIDTH / 2;
        int CENTER_Y = HEIGHT / 2;

        LinkedList<Node> list = new LinkedList<Node>();

        for (int i = 1; i <= nodes; i++) {

            try {
                Node node = type.newInstance();
                node.ID = i;
                list.add(node);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        // shuffle list
        if (random)
            Collections.shuffle(list, new Random(SEED));

        if (nodes>1) {
        	
        	// connect neighbors
            Node previous = null;

            int i = 1;

            for (Node node : list) {

                node.setPosition(CENTER_X - WIDTH / 2 + (WIDTH / (nodes - 1)) * (i - 1), CENTER_Y, 0);

                if (previous != null) {
                    previous.addConnectionTo(node);
                    node.addConnectionTo(previous);
                }

                previous = node;
                i++;
            }
        	
        }
        
        

        return list;
    }

    public static List<Node> createSingleHop(Node type, int nodes) {

        // determine dimensions
        int CENTER_X = WIDTH / 2;
        int CENTER_Y = HEIGHT / 2;

        LinkedList<Node> list = new LinkedList<Node>();

        // define the root tree
        Node root = null;
        try {
            root = type.getClass().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        root.ID = 1;
        root.setPosition(CENTER_X, CENTER_Y - HEIGHT / 2 + 100, 0);

        list.add(root);

        // int depth = Math.floor(Math.log10(nodes - 1)/Math.log10(2);

        for (int i = 2; i <= nodes; i++) {

            try {
                Node node = (Node) type.getClass().newInstance();
                node.ID = i;
                node.setPosition(CENTER_X - WIDTH / 2 + (WIDTH / (nodes - 2)) * (i - 2), CENTER_Y + 100, 0);
                node.addConnectionTo(root);
                root.addConnectionTo(node);
                list.add(node);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return list;
    }

    /*
     * public static void createBinaryTree(int nodes) { Network network =
     * Network.getInstance(); // determine dimensions int WIDTH =
     * WIDTH; int HEIGHT = HEIGHT; int CENTER_X =
     * WIDTH/2; int CENTER_Y = HEIGHT/2; Node root = new Node(1);
     * root.setPosition(CENTER_X, 0); network.addNode(root); subTree(network,
     * nodes, root, 1); } private static void subTree(Network network, int
     * nodes, Node root, int level) { int totalHeight =
     * (int)Math.floor(Math.log10(nodes)/Math.log10(2)); int xspacing =
     * WIDTH/((int)Math.pow(2, level)); int yspacing =
     * HEIGHT/totalHeight; if (2*root.getIdentifier()<=nodes) {
     * Node left = new Node(2*root.getIdentifier());
     * left.setPosition(root.getX() - xspacing/2, root.getY() + yspacing);
     * root.addConnectionTo(left); left.addConnectionTo(root); network.addNode(left);
     * subTree(network, nodes, left, level + 1); } if
     * (2*root.getIdentifier()+1<=nodes) { Node right = new
     * Node(2*root.getIdentifier()+1); right.setPosition(root.getX() +
     * xspacing/2, root.getY() + yspacing); root.addConnectionTo(right);
     * right.addConnectionTo(root); network.addNode(right); subTree(network, nodes,
     * right, level + 1); } }
     */

}

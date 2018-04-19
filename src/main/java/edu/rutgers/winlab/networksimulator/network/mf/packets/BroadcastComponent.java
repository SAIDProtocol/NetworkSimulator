package edu.rutgers.winlab.networksimulator.network.mf.packets;

import edu.rutgers.winlab.networksimulator.network.Node;
import java.util.HashSet;

/**
 *
 * @author Jiachen Chen
 */
public class BroadcastComponent {

    private final HashSet<Node> traversedNodes = new HashSet<>();

    /**
     * Add a node to the traversed nodes.
     *
     * @param n the node to be added
     * @return true if the node is a new node.
     */
    public boolean addNode(Node n) {
        return traversedNodes.add(n);
    }
}

package edu.rutgers.winlab.networksimulator.network.mf.packets;

import edu.rutgers.winlab.networksimulator.common.Data;
import edu.rutgers.winlab.networksimulator.network.Node;

/**
 *
 * @author Jiachen Chen
 */
public class NA implements Data {

    public static final int NA_SIZE = 4;

    private final Node node;

    public NA(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

    @Override
    public int getSizeInBits() {
        return NA_SIZE;
    }
}

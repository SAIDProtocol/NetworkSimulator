package edu.rutgers.winlab.networksimulator.network.mf.packets;

import edu.rutgers.winlab.networksimulator.common.Data;
import edu.rutgers.winlab.networksimulator.network.Node;
import java.util.Objects;

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

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.node);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NA other = (NA) obj;
        return Objects.equals(this.node, other.node);
    }   
}

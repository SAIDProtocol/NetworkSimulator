package edu.rutgers.winlab.networksimulator.network.mf.packets;

import edu.rutgers.winlab.networksimulator.common.Data;

/**
 *
 * @author Jiachen Chen
 */
public class GUID implements Data {

    public static final int GUID_SIZE = 20 * BYTE;

    private final int representation;

    public GUID(int representation) {
        this.representation = representation;
    }

    public int getRepresentation() {
        return representation;
    }

    @Override
    public int getSizeInBits() {
        return GUID_SIZE;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + this.representation;
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
        final GUID other = (GUID) obj;
        return this.representation == other.representation;
    }

}

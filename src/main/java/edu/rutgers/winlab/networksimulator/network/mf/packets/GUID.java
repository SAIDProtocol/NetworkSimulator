package edu.rutgers.winlab.networksimulator.network.mf.packets;

import edu.rutgers.winlab.networksimulator.common.Data;

/**
 *
 * @author Jiachen Chen
 */
public class GUID implements Data {

    public static final int GUID_SIZE = 20;

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
}

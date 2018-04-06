package edu.rutgers.winlab.networksimulator.network.mf.packets;

import edu.rutgers.winlab.networksimulator.common.Data;

/**
 *
 * @author Jiachen Chen
 */
public abstract class MFHopPacket implements Data {
    // Hop Header (16)
    //  type: 4
    //  pld_size: 4
    //  seq_num: 4
    //  hop_ID: 4

    public static final int MF_HOP_HEADER_SIZE = 16;

    private final int type;

    public MFHopPacket(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}

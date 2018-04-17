package edu.rutgers.winlab.networksimulator.network.mf.packets;

/**
 *
 * @author Jiachen Chen
 */
public abstract class MFHopPacketGNRS extends MFHopPacket {

    public static final int MF_HOP_PACKET_GNRS_HEADER_SIZE
            = MF_HOP_HEADER_SIZE + GUID.GUID_SIZE + NA.NA_SIZE + 4 * BYTE;

    private final GUID guid;
    private final NA na;

    public MFHopPacketGNRS(int type, GUID guid, NA na) {
        super(type);
        this.guid = guid;
        this.na = na;
    }

    public GUID getGuid() {
        return guid;
    }

    public NA getNa() {
        return na;
    }
}

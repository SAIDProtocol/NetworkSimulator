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
    private final int serviceId;

    public MFHopPacketGNRS(int type, GUID guid, NA na, int serviceId) {
        super(type);
        this.guid = guid;
        this.na = na;
        this.serviceId = serviceId;
    }

    public GUID getGuid() {
        return guid;
    }

    public NA getNa() {
        return na;
    }

    public int getServiceId() {
        return serviceId;
    }
}

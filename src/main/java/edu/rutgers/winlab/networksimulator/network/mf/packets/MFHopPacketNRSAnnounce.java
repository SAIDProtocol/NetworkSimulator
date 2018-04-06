package edu.rutgers.winlab.networksimulator.network.mf.packets;

/**
 *
 * @author Jiachen Chen
 */
public class MFHopPacketNRSAnnounce extends MFHopPacket {

    public static final int MF_PACKET_TYPE_NRS_ANNOUNCE = 0x51;
    public static final int MF_HOP_PACKET_NRS_ANNOUNCE_SIZE = MF_HOP_HEADER_SIZE + GUID.GUID_SIZE + NA.NA_SIZE + 4;

    private final GUID guid;
    private final NA na;
    private final int version;

    public MFHopPacketNRSAnnounce(GUID guid, NA na, int version) {
        super(MF_PACKET_TYPE_NRS_ANNOUNCE);
        this.guid = guid;
        this.na = na;
        this.version = version;
    }

    public GUID getGuid() {
        return guid;
    }

    public NA getNa() {
        return na;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public int getSizeInBits() {
        return MF_HOP_PACKET_NRS_ANNOUNCE_SIZE;
    }

}

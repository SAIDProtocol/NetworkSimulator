package edu.rutgers.winlab.networksimulator.network.mf.packets;

/**
 *
 * @author Jiachen Chen
 */
public class MFHopPacketGNRSResponse extends MFHopPacketGNRS {

    public static final int MF_PACKET_TYPE_GNRS_RESPONSE = 0x61;

    public static final int MF_HOP_PACKET_GNRS_RESPONSE_SIZE_BASE
            = MF_HOP_PACKET_GNRS_HEADER_SIZE + 4 * BYTE * 2;

    // private final int nas.length;
    private final NA nas[];
    private final int version;

    public MFHopPacketGNRSResponse(GUID guid, NA na, NA[] nas, int version) {
        super(MF_PACKET_TYPE_GNRS_RESPONSE, guid, na);
        this.nas = nas;
        this.version = version;
    }

    public NA[] getNas() {
        return nas;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public int getSizeInBits() {
        return MF_HOP_PACKET_GNRS_RESPONSE_SIZE_BASE + nas.length * NA.NA_SIZE;
    }

}

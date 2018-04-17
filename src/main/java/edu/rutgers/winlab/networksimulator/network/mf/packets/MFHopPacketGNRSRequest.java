package edu.rutgers.winlab.networksimulator.network.mf.packets;

/**
 *
 * @author Jiachen Chen
 */
public class MFHopPacketGNRSRequest extends MFHopPacketGNRS {

    public static final int MF_PACKET_TYPE_GNRS_REQUEST = 0x60;

    public static final int MF_HOP_PACKET_GNRS_REQUEST_SIZE = MF_HOP_PACKET_GNRS_HEADER_SIZE;

    public MFHopPacketGNRSRequest(GUID guid, NA na) {
        super(MF_PACKET_TYPE_GNRS_REQUEST, guid, na);
    }

    @Override
    public int getSizeInBits() {
        return MF_HOP_PACKET_GNRS_REQUEST_SIZE;
    }

}

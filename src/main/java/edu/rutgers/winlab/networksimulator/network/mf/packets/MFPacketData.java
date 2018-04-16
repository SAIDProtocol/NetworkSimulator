package edu.rutgers.winlab.networksimulator.network.mf.packets;

import edu.rutgers.winlab.networksimulator.common.Data;

/**
 *
 * @author Jiachen Chen
 */
public final class MFPacketData extends MFApplicationPacket {

    public static final int MF_PACKET_TYPE_DATA = 0x0;

    private final Data payload;

    public MFPacketData(GUID src, GUID dst, NA srcNA, NA dstNA, Data payload) {
        super(MF_PACKET_TYPE_DATA, src, dst, srcNA, dstNA);
        this.payload = payload;
    }

    public MFPacketData(GUID src, GUID dst, Data payload) {
        this(src, dst, null, null, payload);
    }

    public MFPacketData setNAs(NA srcNA, NA dstNA) {
        return new MFPacketData(getSrc(), getDst(), srcNA, dstNA, payload);
    }

    @Override
    public int getSizeInBits() {
        return MF_APPLICATION_PACKET_HEADER_SIZE + payload.getSizeInBits();
    }
}

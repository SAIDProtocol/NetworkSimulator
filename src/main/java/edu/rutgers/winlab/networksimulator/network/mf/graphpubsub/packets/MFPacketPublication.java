package edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets;

import edu.rutgers.winlab.networksimulator.network.mf.packets.NA;
import edu.rutgers.winlab.networksimulator.network.mf.packets.GUID;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFPacket;
import edu.rutgers.winlab.networksimulator.common.Data;

/**
 * A publication packet.
 *
 * MFPacket.src -> publisher GUID.
 *
 * MFPacket.dst -> target group GUID
 *
 * MFPacket.srcNA -> null if from publisher, otherwise from RP.
 * 
 * MFPacket.dstNA -> RPNA
 *
 * @author Jiachen Chen
 */
public final class MFPacketPublication extends MFPacket {

    private Data payload;
    public static final int MF_PACKET_TYPE_PUBLICATION = 0x100;

    public MFPacketPublication(GUID src, GUID dst, NA rpNA, Data payload) {
        super(MF_PACKET_TYPE_PUBLICATION, src, dst, null, rpNA);
    }

    public MFPacketPublication(GUID src, GUID dst, Data payload) {
        this(src, dst, null, payload);
    }

    public NA getRpNA() {
        return getDstNA();
    }

    public Data getPayload() {
        return payload;
    }

    public MFPacketPublication fillRPNA(NA rpNA) {
        return new MFPacketPublication(getSrc(), getDst(), rpNA, payload);
    }

    @Override
    public int getSizeInBits() {
        return MF_PACKET_HEADER_SIZE + payload.getSizeInBits();
    }
}

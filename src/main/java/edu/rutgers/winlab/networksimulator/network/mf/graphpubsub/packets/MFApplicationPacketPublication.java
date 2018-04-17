package edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets;

import edu.rutgers.winlab.networksimulator.network.mf.packets.NA;
import edu.rutgers.winlab.networksimulator.network.mf.packets.GUID;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFApplicationPacket;
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
public final class MFApplicationPacketPublication extends MFApplicationPacket {

    private Data payload;
    public static final int MF_PACKET_TYPE_PUBLICATION = 0x100;

    public MFApplicationPacketPublication(GUID src, GUID dst, NA rpNA, Data payload) {
        super(MF_PACKET_TYPE_PUBLICATION, src, dst, null, rpNA);
    }

    public MFApplicationPacketPublication(GUID src, GUID dst, Data payload) {
        this(src, dst, null, payload);
    }

    public NA getRpNA() {
        return getDstNA();
    }

    public Data getPayload() {
        return payload;
    }

    public MFApplicationPacketPublication fillRPNA(NA rpNA) {
        return new MFApplicationPacketPublication(getSrc(), getDst(), rpNA, payload);
    }

    @Override
    public int getSizeInBits() {
        return MF_APPLICATION_PACKET_HEADER_SIZE + payload.getSizeInBits();
    }

    @Override
    public MFApplicationPacket copyWithNewDstNa(NA newDstNa) {
        return new MFApplicationPacketPublication(getSrc(), getDst(), newDstNa, payload);
    }
}

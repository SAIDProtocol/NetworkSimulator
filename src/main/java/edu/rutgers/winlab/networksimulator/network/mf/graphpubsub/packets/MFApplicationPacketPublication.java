package edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets;

import edu.rutgers.winlab.networksimulator.network.mf.packets.NA;
import edu.rutgers.winlab.networksimulator.network.mf.packets.GUID;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFApplicationPacket;
import edu.rutgers.winlab.networksimulator.common.Data;
import edu.rutgers.winlab.networksimulator.network.Node;
import edu.rutgers.winlab.networksimulator.network.mf.packets.BroadcastComponent;

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

    public static final int MF_PACKET_TYPE_PUBLICATION = 0x100;

    private Data payload;
    private BroadcastComponent broadcast = new BroadcastComponent();

    public MFApplicationPacketPublication(GUID src, GUID dst, NA rpNA, Data payload) {
        this(src, dst, null, rpNA, payload);
    }

    public MFApplicationPacketPublication(GUID src, GUID dst, Data payload) {
        this(src, dst, null, payload);
    }

    private MFApplicationPacketPublication(GUID src, GUID dst, NA srcNA, NA dstNA, Data payload) {
        super(MF_PACKET_TYPE_PUBLICATION, src, dst, srcNA, dstNA);
        this.payload = payload;
    }

    public Data getPayload() {
        return payload;
    }

    public boolean addNode(Node n) {
        return broadcast.addNode(n);
    }

    @Override
    public int getSizeInBits() {
        return MF_APPLICATION_PACKET_HEADER_SIZE + payload.getSizeInBits();
    }

    public MFApplicationPacketPublication copyWithNewSrcNa(NA newSrcNa) {
        return new MFApplicationPacketPublication(getSrc(), getDst(), newSrcNa, getDstNA(), payload);
    }

    @Override
    public MFApplicationPacket copyWithNewDstNa(NA newDstNa) {
        return new MFApplicationPacketPublication(getSrc(), getDst(), newDstNa, payload);
    }

    /**
     * Used when RP copying a publication packet to another group, and forward
     *
     * @param newDst the new group GUID
     * @param newSrcNa the RP's na
     * @return the new packet
     */
    public MFApplicationPacketPublication copyWithNewDstGUIDAndSrcNa(GUID newDst, NA newSrcNa) {
        return new MFApplicationPacketPublication(getSrc(), newDst, newSrcNa, getDstNA(), payload);
    }
}

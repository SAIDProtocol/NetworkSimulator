package edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets;

import edu.rutgers.winlab.networksimulator.network.mf.packets.NA;
import edu.rutgers.winlab.networksimulator.network.mf.packets.GUID;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFPacket;

/**
 * A subscription packet.
 * 
 * MFPacket.src -> subscriber.
 * 
 * MFPacket.dst -> target group (name).
 * 
 * MFPacket.srcNA -> null.
 * 
 * MFPacket.dstNA -> RPNA.
 * 
 * @author Jiachen Chen
 */
public final class MFPacketSubscription extends MFPacket {

    public static final int MF_PACKET_TYPE_SUBSCRIPTION = 0x101;
    public static final int SUBSCRPTION_SIZE = MF_PACKET_HEADER_SIZE;

    public MFPacketSubscription(GUID subscriber, GUID name) {
        this(subscriber, name, null);
    }

    public MFPacketSubscription(GUID subscriber, GUID name, NA rpNA) {
        super(MF_PACKET_TYPE_SUBSCRIPTION, subscriber, name, null, rpNA);
    }

    public GUID getSubscriber() {
        return getSrc();
    }

    public GUID getName() {
        return getDst();
    }

    public NA getRPNA() {
        return getDstNA();
    }

    public MFPacketSubscription fillRPNA(NA newRPNA) {
        return new MFPacketSubscription(getSubscriber(), getName(), newRPNA);
    }

    @Override
    public int getSizeInBits() {
        return SUBSCRPTION_SIZE;
    }
}

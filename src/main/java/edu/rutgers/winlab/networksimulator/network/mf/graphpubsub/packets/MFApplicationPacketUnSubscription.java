package edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets;

import edu.rutgers.winlab.networksimulator.network.mf.packets.GUID;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFApplicationPacket;
import static edu.rutgers.winlab.networksimulator.network.mf.packets.MFApplicationPacket.MF_APPLICATION_PACKET_HEADER_SIZE;
import edu.rutgers.winlab.networksimulator.network.mf.packets.NA;

/**
 *
 * @author Jiachen Chen
 */
public class MFApplicationPacketUnSubscription extends MFApplicationPacket {

    public static final int MF_PACKET_TYPE_UNSUBSCRIPTION = 0x105;
    public static final int SUBSCRPTION_SIZE = MF_APPLICATION_PACKET_HEADER_SIZE;

    public MFApplicationPacketUnSubscription(GUID subscriber, GUID name) {
        this(subscriber, name, null);
    }

    public MFApplicationPacketUnSubscription(GUID subscriber, GUID name, NA rpNA) {
        super(MF_PACKET_TYPE_UNSUBSCRIPTION, subscriber, name, null, rpNA);
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

    public MFApplicationPacketUnSubscription fillRPNA(NA newRPNA) {
        return new MFApplicationPacketUnSubscription(getSubscriber(), getName(), newRPNA);
    }

    @Override
    public int getSizeInBits() {
        return SUBSCRPTION_SIZE;
    }

    @Override
    public MFApplicationPacket copyWithNewDstNa(NA newDstNa) {
        return new MFApplicationPacketUnSubscription(getSrc(), getDst(), newDstNa);
    }

}

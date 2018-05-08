package edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets;

import edu.rutgers.winlab.networksimulator.network.mf.packets.GUID;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFApplicationPacket;
import edu.rutgers.winlab.networksimulator.network.mf.packets.NA;

/**
 *
 * @author Jiachen Chen
 */
public class MFApplicationPacketMark2 extends MFApplicationPacket {

    public static final int MF_PACKET_TYPE_MARK_2 = 0x104;

    public MFApplicationPacketMark2(GUID guid, NA srcNA, NA dstNA) {
        super(MF_PACKET_TYPE_MARK_2, null, guid, srcNA, dstNA);
    }

    public GUID getGUID() {
        return getDst();
    }

    @Override
    public MFApplicationPacket copyWithNewDstNa(NA newDstNa) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getSizeInBits() {
        return MF_APPLICATION_PACKET_HEADER_SIZE;
    }

}

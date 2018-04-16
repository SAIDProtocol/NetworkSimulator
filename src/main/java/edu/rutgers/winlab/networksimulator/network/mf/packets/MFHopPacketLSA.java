package edu.rutgers.winlab.networksimulator.network.mf.packets;

/**
 *
 * @author Jiachen Chen
 */
public class MFHopPacketLSA extends MFHopPacket {

    public static final int MF_PACKET_TYPE_LSA = 0x50;

    private final NA na;
    // this design violates the network design, used just to create a routing layer
    private final long sendTime;

    // NA: NA.NA_SIZE
    // Nonce: 4 (prevent loop)
    public static final int MF_HOP_PACKET_LSA_SIZE
            = MF_HOP_HEADER_SIZE + NA.NA_SIZE + 4 * BYTE;

    public MFHopPacketLSA(NA na, long sendTime) {
        super(MF_PACKET_TYPE_LSA);
        this.na = na;
        this.sendTime = sendTime;
    }

    public NA getNa() {
        return na;
    }

    public long getSendTime() {
        return sendTime;
    }

    @Override
    public int getSizeInBits() {
        return MF_HOP_PACKET_LSA_SIZE;
    }

}

package edu.rutgers.winlab.networksimulator.network.mf.packets;

/**
 *
 * @author Jiachen Chen
 */
public class MFHopPacketLSA extends MFHopPacket {

    public static final int MF_PACKET_TYPE_LSA = 0x50;

    private final NA na;

    // NA: NA.NA_SIZE
    // Nonce: 4 (prevent loop)
    public static final int MF_HOP_PACKET_LSA_SIZE = MF_HOP_HEADER_SIZE + NA.NA_SIZE + 4;

    public MFHopPacketLSA(NA na) {
        super(MF_PACKET_TYPE_LSA);
        this.na = na;
    }

    public NA getNa() {
        return na;
    }

    @Override
    public int getSizeInBits() {
        return MF_HOP_PACKET_LSA_SIZE;
    }

}

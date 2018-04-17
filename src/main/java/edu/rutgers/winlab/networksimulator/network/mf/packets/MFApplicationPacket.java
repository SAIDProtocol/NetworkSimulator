package edu.rutgers.winlab.networksimulator.network.mf.packets;

/**
 * A normal MobilityFirst packet
 *
 * @author Jiachen Chen
 */
public abstract class MFApplicationPacket extends MFHopPacket {

    // Routing Header (60)
    //  Version: 1
    //  Service Identifier: 2
    //  Protocol: 1
    //  Payload offset: 2
    //  Reserved: 2
    //  Payload size: 4
    //  Source GUID: GUID.GUID_SIZE 
    //  Source NA: NA.NA_SIZE
    //  Destination GUID: GUID.GUID_SIZE
    //  Destination NA: NA.NA_SIZE
    //  Extension Headers (none)
    public static final int MF_APPLICATION_PACKET_HEADER_SIZE
            = MF_HOP_HEADER_SIZE + 2 * GUID.GUID_SIZE + 2 * NA.NA_SIZE + 12 * BYTE;

    private final GUID src, dst;
    private final NA srcNA, dstNA;

    public MFApplicationPacket(int type, GUID src, GUID dst, NA srcNA, NA dstNA) {
        super(type);
        this.src = src;
        this.dst = dst;
        this.srcNA = srcNA;
        this.dstNA = dstNA;
    }

    public GUID getSrc() {
        return src;
    }

    public GUID getDst() {
        return dst;
    }

    public NA getSrcNA() {
        return srcNA;
    }

    public NA getDstNA() {
        return dstNA;
    }

    public abstract MFApplicationPacket copyWithNewDstNa(NA newDstNa);

}

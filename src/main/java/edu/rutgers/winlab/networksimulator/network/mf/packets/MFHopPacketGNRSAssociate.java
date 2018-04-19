package edu.rutgers.winlab.networksimulator.network.mf.packets;

/**
 *
 * @author Jiachen Chen
 */
public class MFHopPacketGNRSAssociate extends MFHopPacketGNRS {

    public static final int MF_PACKET_TYPE_GNRS_ASSOCIATE = 0x62;

    public static final int MF_HOP_PACKET_GNRS_ASSOCIATE_SIZE_BASE
            = MF_HOP_HEADER_SIZE + GUID.GUID_SIZE + (4 * 2 + 1) * BYTE;

    // private final int nasAdd.length, nasRemove.length
    private final NA[] nasAdd, nasRemove;
    private final boolean broadcastResult;

    public MFHopPacketGNRSAssociate(GUID guid, NA na, NA[] nasAdd, NA[] nasRemove, boolean broadcastResult) {
        super(MF_PACKET_TYPE_GNRS_ASSOCIATE, guid, na);
        this.nasAdd = nasAdd;
        this.nasRemove = nasRemove;
        this.broadcastResult = broadcastResult;
    }

    public NA[] getNasAdd() {
        return nasAdd;
    }

    public NA[] getNasRemove() {
        return nasRemove;
    }

    public boolean isBroadcastResult() {
        return broadcastResult;
    }

    @Override
    public int getSizeInBits() {
        return MF_HOP_PACKET_GNRS_ASSOCIATE_SIZE_BASE + (nasAdd.length + nasRemove.length) * NA.NA_SIZE;
    }

}

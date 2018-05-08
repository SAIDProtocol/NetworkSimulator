package edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets;

import edu.rutgers.winlab.networksimulator.common.Data;
import edu.rutgers.winlab.networksimulator.network.mf.packets.GUID;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFApplicationPacket;
import edu.rutgers.winlab.networksimulator.network.mf.packets.NA;
import java.util.HashSet;

/**
 *
 * @author Jiachen Chen
 */
public class MFApplicationPacketNotifyRP extends MFApplicationPacket {

    public static final int MF_PACKET_TYPE_NOTIFY_RP = 0x102;

    private final HashSet<GUID> children;

    public MFApplicationPacketNotifyRP(GUID guid, HashSet<GUID> children, NA srcNA, NA dstNA) {
        super(MF_PACKET_TYPE_NOTIFY_RP, null, guid, srcNA, dstNA);
        this.children = children;
    }

    public HashSet<GUID> getChildren() {
        return children;
    }

    public GUID getGUID() {
        return getDst();
    }

    @Override
    public MFApplicationPacket copyWithNewDstNa(NA newDstNa) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getSizeInBits() {
        return MF_APPLICATION_PACKET_HEADER_SIZE + children.size() * GUID.GUID_SIZE + 4 * Data.BYTE;
    }

}

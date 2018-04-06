package edu.rutgers.winlab.networksimulator.network.mf;

import edu.rutgers.winlab.networksimulator.common.Data;
import edu.rutgers.winlab.networksimulator.common.PrioritizedQueue;
import edu.rutgers.winlab.networksimulator.common.Timeline;
import edu.rutgers.winlab.networksimulator.common.Tuple2;
import edu.rutgers.winlab.networksimulator.network.Node;
import edu.rutgers.winlab.networksimulator.network.mf.packets.GUID;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFHopPacket;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFHopPacketLSA;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFPacketData;
import edu.rutgers.winlab.networksimulator.network.mf.packets.NA;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 *
 * @author Jiachen Chen
 */
public class MFRouter extends Node {

    public static final long DURATION_HANDLE_LSA = 1 * Timeline.MS;
    public static final long DURATION_HANDLE_DATA = 5 * Timeline.MS;

    private final NA na;
    private final HashMap<Node, Node> fib = new HashMap<>();
    private final HashMap<GUID, Tuple2<Node, Integer>> nrs = new HashMap<>();

    public MFRouter(String name, PrioritizedQueue<Tuple2<Node, Data>> incomingQueue) {
        super(name, incomingQueue);
        na = new NA(this);
    }

    @Override
    protected long handleData(Tuple2<Node, Data> t) {
        Node source = t.getV1();
        MFHopPacket p = (MFHopPacket) t.getV2();
        switch (p.getType()) {
            case MFHopPacketLSA.MF_PACKET_TYPE_LSA:
                return handleLSA(source, (MFHopPacketLSA) p);
            case MFPacketData.MF_PACKET_TYPE_DATA:
                return handleData(source, (MFPacketData) p);
            default:
                handleOtherData(source, p);
        }
        return 0;
    }

    private long handleLSA(Node src, MFHopPacketLSA packet) {
        Node target = packet.getNa().getNode();
        if (!fib.containsKey(target)) {
            fib.put(target, src);
            Timeline.addEvent(DURATION_HANDLE_LSA, ps -> {
                forEachLink(l -> sendData(l, (Data) ps[0], true));
            }, packet);
        }
        return DURATION_HANDLE_LSA;
    }

    private long handleData(Node src, MFPacketData data) {

        return DURATION_HANDLE_DATA;
    }

    public Stream<Entry<Node, Node>> fibStream() {
        return fib.entrySet().stream();
    }

    public void forEachFib(BiConsumer<? super Node, ? super Node> consumer) {
        fib.forEach(consumer);
    }

    protected long handleOtherData(Node src, MFHopPacket packet) {
        return 0;
    }

    public void announceNA() {
        forEachLink(l -> sendData(l, new MFHopPacketLSA(na), true));
    }

}

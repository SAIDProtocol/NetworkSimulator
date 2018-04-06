package edu.rutgers.winlab.networksimulator.network.mf;

import edu.rutgers.winlab.networksimulator.common.Data;
import edu.rutgers.winlab.networksimulator.common.PrioritizedQueue;
import edu.rutgers.winlab.networksimulator.common.Timeline;
import edu.rutgers.winlab.networksimulator.common.Tuple2;
import edu.rutgers.winlab.networksimulator.network.Node;
import edu.rutgers.winlab.networksimulator.network.mf.packets.GUID;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFHopPacket;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFHopPacketLSA;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFHopPacketNRSAnnounce;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFPacket;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFPacketData;
import edu.rutgers.winlab.networksimulator.network.mf.packets.NA;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 *
 * @author Jiachen Chen
 */
public class MFRouter extends Node {

    public static final long DURATION_HANDLE_LSA = 1 * Timeline.MS;
    public static final long DURATION_HANDLE_NRS_ANNOUNCE = 1 * Timeline.MS;
    public static final long DURATION_HANDLE_DATA = 5 * Timeline.MS;

    private final NA na;
    private final HashMap<NA, Node> fib = new HashMap<>();
    private final HashMap<GUID, Tuple2<NA, Integer>> nrs = new HashMap<>();
    private final HashSet<MFPacket> nrsPending = new HashSet<>();

    public MFRouter(String name, PrioritizedQueue<Tuple2<Node, Data>> incomingQueue) {
        super(name, incomingQueue);
        na = new NA(this);
    }

    public Stream<Entry<NA, Node>> fibStream() {
        return fib.entrySet().stream();
    }

    public void forEachFib(BiConsumer<? super NA, ? super Node> consumer) {
        fib.forEach(consumer);
    }

    public void clearFib() {
        fib.clear();
    }

    public Stream<Entry<GUID, Tuple2<NA, Integer>>> nrsStream() {
        return nrs.entrySet().stream();
    }

    public void forEachNrs(BiConsumer<? super GUID, ? super Tuple2<NA, Integer>> consumer) {
        nrs.forEach(consumer);
    }

    public void announceNA() {
        fib.put(na, this);
        forEachLink(l -> sendData(l, new MFHopPacketLSA(na), true));
    }

    @Override
    protected long handleData(Tuple2<Node, Data> t) {
        Node source = t.getV1();
        MFHopPacket p = (MFHopPacket) t.getV2();
        switch (p.getType()) {
            case MFHopPacketLSA.MF_PACKET_TYPE_LSA:
                return handleMFLSA(source, (MFHopPacketLSA) p);
            case MFPacketData.MF_PACKET_TYPE_DATA:
                return handleMFData(source, (MFPacketData) p);
            case MFHopPacketNRSAnnounce.MF_PACKET_TYPE_NRS_ANNOUNCE:
                return handleMFNRSAnnounce(source, (MFHopPacketNRSAnnounce) p);
            default:
                handleOtherData(source, p);
        }
        return 0;
    }

    protected long handleMFLSA(Node src, MFHopPacketLSA packet) {
        NA target = packet.getNa();
        if (!fib.containsKey(target)) {
            fib.put(target, src);
            Timeline.addEvent(DURATION_HANDLE_LSA, ps -> {
                forEachLink(l -> sendData(l, (Data) ps[0], true));
            }, packet);
        }
        return DURATION_HANDLE_LSA;
    }

    protected long handleMFNRSAnnounce(Node src, MFHopPacketNRSAnnounce packet) {
        GUID target = packet.getGuid();
        NA targetNa = packet.getNa();
        int version = packet.getVersion();
        Tuple2<NA, Integer> t = nrs.get(target);
        process:
        {
            if (t == null) {
                nrs.put(target, new Tuple2<>(packet.getNa(), packet.getVersion()));
            } else if (t.getV2() < version) {
                t.setV1(targetNa);
                t.setV2(version);
            } else {
                break process;
            }
            // remove all pending data whose guid is target, add them to toSend
            ArrayList<MFPacket> toSend = new ArrayList<>();
            Node nextHop = fib.get(targetNa);
            nrsPending.removeIf(x -> {
                if (x.getDst() == target) {
                    toSend.add(x);
                    return true;
                }
                return false;
            });
            Timeline.addEvent(DURATION_HANDLE_NRS_ANNOUNCE, ps -> {
                forEachLink(l -> sendData(l, (Data) ps[0], true));
                // send pending Packets to ToSend
                Node nh = (Node) ps[2];
                ((ArrayList<MFPacket>) ps[1]).forEach((pkt) -> {
                    sendData(nh, pkt, false);
                });
            }, packet, toSend, nextHop);

        }
        return DURATION_HANDLE_NRS_ANNOUNCE;
    }

    protected long handleMFData(Node src, MFPacketData data) {
        long ret;
        process:
        {
            if (data.getDstNA() == null) {
                Tuple2<NA, Integer> t = nrs.get(data.getDst());
                if (t == null) {
                    // no NRS entry here, add to pending list
                    nrsPending.add(data);
                    ret = DURATION_HANDLE_DATA;
                    break process;
                } else {
                    data = data.setNAs(data.getSrcNA(), t.getV1());
                }
            }
            // i should handle the packet myself
            if (data.getDstNA().getNode() == this) {
                ret = 0;
                break process;
            }
            ret = DURATION_HANDLE_DATA;
            Node target = fib.get(data.getDstNA());
            Timeline.addEvent(DURATION_HANDLE_NRS_ANNOUNCE, ps -> {
                sendData((Node) ps[0], (Data) ps[1], false);
            }, target, data);
        }
        return ret;
    }

    protected long handleMFDataSelf(Node src, MFPacketData data) {
        return 0;
    }

    protected long handleOtherData(Node src, MFHopPacket packet) {
        return 0;
    }

}

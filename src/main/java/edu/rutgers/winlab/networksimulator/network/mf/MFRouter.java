package edu.rutgers.winlab.networksimulator.network.mf;

import edu.rutgers.winlab.networksimulator.common.Data;
import edu.rutgers.winlab.networksimulator.common.PrioritizedQueue;
import edu.rutgers.winlab.networksimulator.common.Timeline;
import edu.rutgers.winlab.networksimulator.common.Tuple2;
import edu.rutgers.winlab.networksimulator.common.Tuple3;
import edu.rutgers.winlab.networksimulator.network.Node;
import edu.rutgers.winlab.networksimulator.network.mf.packets.GUID;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFHopPacketLSA;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFHopPacketGNRSResponse;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFApplicationPacket;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFHopPacket;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFHopPacketGNRSAssociate;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFHopPacketGNRSRequest;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFPacketData;
import edu.rutgers.winlab.networksimulator.network.mf.packets.NA;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 * @author Jiachen Chen
 */
public class MFRouter extends Node {

    public static final long DURATION_HANDLE_LSA = 100 * Timeline.US;
    public static final long DURATION_HANDLE_NA_FORWARDING = 200 * Timeline.US;
//    public static final long DURATION_HANDLE_GNRS_FORWARDING = 5 * Timeline.MS;

    private final NA na;
    private final NA gnrsNa;
    private final HashMap<NA, Tuple2<Node, Long>> fib = new HashMap<>();
    private final HashMap<GUID, HashSet<BiConsumer<? super MFRouter, ? super MFPacketData>>> dataConsumers = new HashMap<>();
    private final HashMap<GUID, Tuple3<NA[], Integer, Long>> nrsCache = new HashMap<>();
    private final HashSet<MFApplicationPacket> nrsPending = new HashSet<>();

    public MFRouter(String name, PrioritizedQueue<Tuple2<Node, Data>> incomingQueue, NA gnrsNa) {
        super(name, incomingQueue);
        na = new NA(this);
        if (gnrsNa == null || !(gnrsNa.getNode() instanceof MFGNRS)) {
            throw new IllegalArgumentException("GNRS target should be a GNRS node!");
        }
        this.gnrsNa = gnrsNa;
    }

    protected MFRouter(String name, PrioritizedQueue<Tuple2<Node, Data>> incomingQueue) {
        super(name, incomingQueue);
        na = new NA(this);
        if (!(this instanceof MFGNRS)) {
            throw new IllegalArgumentException("This constructor is only for MFGNRS");
        }
        gnrsNa = na;
    }

    public final NA getNa() {
        return na;
    }

    public final NA getGnrsNa() {
        return gnrsNa;
    }

    public final Stream<Entry<NA, Tuple2<Node, Long>>> fibStream() {
        return fib.entrySet().stream();
    }

    public final void forEachFib(BiConsumer<? super NA, ? super Tuple2<Node, Long>> consumer) {
        fib.forEach(consumer);
    }

    public final void clearFib() {
        fib.clear();
    }

    public final Stream<Entry<GUID, Tuple3<NA[], Integer, Long>>> nrsCacheStream() {
        return nrsCache.entrySet().stream();
    }

    public final Stream<Entry<GUID, Tuple3<NA[], Integer, Long>>> activeNrsCacheStream() {
        long now = Timeline.nowInUs();
        return nrsCache.entrySet().stream().filter(e -> e.getValue().getV3() >= now);
    }

    public final void forEachNrsCache(BiConsumer<? super GUID, ? super Tuple3<NA[], Integer, Long>> consumer) {
        nrsCache.forEach(consumer);
    }

    public final void registerDataConsumer(GUID guid, BiConsumer<? super MFRouter, ? super MFPacketData> consumer) {
        HashSet<BiConsumer<? super MFRouter, ? super MFPacketData>> list = new HashSet<>(), tmp;
        tmp = dataConsumers.putIfAbsent(guid, list);
        if (tmp != null) {
            list = tmp;
        } else {
            //associate
            MFHopPacketGNRSAssociate assoc = new MFHopPacketGNRSAssociate(guid, na, 0, new NA[]{na}, new NA[0], false);
            enqueueIncomingData(this, assoc);
        }
        list.add(consumer);
    }

    public final void deregisterDataConsumer(GUID guid, BiConsumer<? super MFRouter, ? super MFPacketData> consumer) {
        HashSet<BiConsumer<? super MFRouter, ? super MFPacketData>> list = dataConsumers.get(guid);
        if (list == null) {
            return;
        }
        list.remove(consumer);
        if (list.isEmpty()) {
            dataConsumers.remove(guid);
            //deassociate
            MFHopPacketGNRSAssociate assoc = new MFHopPacketGNRSAssociate(guid, na, 0, new NA[0], new NA[]{na}, false);
            enqueueIncomingData(this, assoc);
        }
    }

    public final void announceNA() {
        fib.put(na, new Tuple2<>(this, 0L));
        MFHopPacketLSA announce = new MFHopPacketLSA(na, Timeline.nowInUs());
        forEachLink(l -> sendData(l, announce, true));
    }

    protected void sendData(NA target, Data data, boolean prioritized) {
        if (target == this.na) {
            enqueueIncomingData(this, data);
            return;
        }
        Tuple2<Node, Long> nextHop = fib.get(target);
        //next hop should not be null
        sendData(nextHop.getV1(), data, prioritized);
    }

    @Override
    protected long handleData(Tuple2<Node, Data> t) {
        Node source = t.getV1();
        MFHopPacket p = (MFHopPacket) t.getV2();
        switch (p.getType()) {
            case MFHopPacketLSA.MF_PACKET_TYPE_LSA:
                return handleMFLSA(source, (MFHopPacketLSA) p);
            case MFHopPacketGNRSRequest.MF_PACKET_TYPE_GNRS_REQUEST:
                return handleGNRSRequest(source, (MFHopPacketGNRSRequest) p);
            case MFHopPacketGNRSAssociate.MF_PACKET_TYPE_GNRS_ASSOCIATE:
                return handleGNRSAssociate(source, (MFHopPacketGNRSAssociate) p);
            case MFHopPacketGNRSResponse.MF_PACKET_TYPE_GNRS_RESPONSE:
                return handleGNRSResponse(source, (MFHopPacketGNRSResponse) p);
            case MFPacketData.MF_PACKET_TYPE_DATA:
                return handleMFData(source, (MFPacketData) p);
//            default:
//                handleOtherData(source, p);
        }
        return 0;
    }

    protected long handleGNRSResponse(Node src, MFHopPacketGNRSResponse packet) {
        NA dst = packet.getNa();
        if (dst == null) {
            // TODO: update local cache, and forward it
        } else {
            sendData(dst, packet, true);
        }
        return DURATION_HANDLE_NA_FORWARDING;
    }

    protected long handleGNRSRequest(Node src, MFHopPacketGNRSRequest packet) {
        sendData(getGnrsNa(), packet, true);
        return DURATION_HANDLE_NA_FORWARDING;
    }

    protected long handleGNRSAssociate(Node src, MFHopPacketGNRSAssociate packet) {
        sendData(getGnrsNa(), packet, true);
        return DURATION_HANDLE_NA_FORWARDING;
    }

    protected long handleMFLSA(Node src, MFHopPacketLSA packet) {
        NA target = packet.getNa();
        if (!fib.containsKey(target)) {
            long duration = Timeline.nowInUs() - packet.getSendTime();
            fib.put(target, new Tuple2<>(src, duration));
            Timeline.addEvent(Timeline.nowInUs() + DURATION_HANDLE_LSA, ps -> {
                forEachLink(l -> sendData(l, (Data) ps[0], true));
            }, packet);
        }
        return DURATION_HANDLE_LSA;
    }

    protected long handleMFNRSAnnounce(Node src, MFHopPacketGNRSResponse packet) {
//        GUID target = packet.getGuid();
//        NA targetNa = packet.getNa();
//        int version = packet.getVersion();
//        Tuple2<NA, Integer> t = nrs.get(target);
//        process:
//        {
//            if (t == null) {
//                nrs.put(target, new Tuple2<>(packet.getNa(), packet.getVersion()));
//            } else if (t.getV2() < version) {
//                t.setV1(targetNa);
//                t.setV2(version);
//            } else {
//                break process;
//            }
//            // remove all pending data whose guid is target, add them to toSend
//            ArrayList<MFPacket> toSend = new ArrayList<>();
//            Node nextHop = fib.get(targetNa);
//            nrsPending.removeIf(x -> {
//                if (x.getDst() == target) {
//                    toSend.add(x);
//                    return true;
//                }
//                return false;
//            });
//            Timeline.addEvent(DURATION_HANDLE_NRS_ANNOUNCE, ps -> {
//                forEachLink(l -> sendData(l, (Data) ps[0], true));
//                // send pending Packets to ToSend
//                Node nh = (Node) ps[2];
//                ((ArrayList<MFPacket>) ps[1]).forEach((pkt) -> {
//                    sendData(nh, pkt, false);
//                });
//            }, packet, toSend, nextHop);
//
//        }
        return 0;
//        return DURATION_HANDLE_NRS_ANNOUNCE;
    }

    protected long handleMFData(Node src, MFPacketData data) {
        long ret;
//        process:
//        {
//            if (data.getDstNA() == null) {
//                Tuple2<NA, Integer> t = nrs.get(data.getDst());
//                if (t == null) {
//                    // no NRS entry here, add to pending list
//                    nrsPending.add(data);
//        ret = DURATION_HANDLE_DATA;
//                    break process;
//                } else {
//                    data = data.setNAs(data.getSrcNA(), t.getV1());
//                }
//            }
//            // i should handle the packet myself
//            if (data.getDstNA().getNode() == this) {
//                ret = 0;
//                break process;
//            }
//            ret = DURATION_HANDLE_DATA;
//            Node target = fib.get(data.getDstNA());
//            Timeline.addEvent(DURATION_HANDLE_NRS_ANNOUNCE, ps -> {
//                sendData((Node) ps[0], (Data) ps[1], false);
//            }, target, data);
//        }
        return 0;
    }
}

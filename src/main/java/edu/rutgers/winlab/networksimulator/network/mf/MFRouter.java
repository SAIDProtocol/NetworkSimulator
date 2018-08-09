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
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFApplicationPacketData;
import edu.rutgers.winlab.networksimulator.network.mf.packets.NA;
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

    public static final long DURATION_HANDLE_LSA = 30 * Timeline.US;
    public static final long DURATION_HANDLE_NA_FORWARDING = 30 * Timeline.US;
    public static final long DURATION_HANDLE_GNRS_RESPONSE = 100 * Timeline.US;
    public static final long DURATION_HANDLE_DATA_FORWARD_TO_APPLICATION = 10 * Timeline.US;
    public static final long DURATION_HANDLE_DATA_STORE_AND_FORWARD = 100 * Timeline.US;
    public static final long DURATION_HANDLE_DATA_ADD_NA_AND_FORWARD = 30 * Timeline.US;
    public static final long DURATION_HANDLE_OTHER = 50 * Timeline.US;
    public static long durationNrsCacheExpire = 3600 * 24 * Timeline.SECOND;
    public static long durationNrsReIssue = 3600 * 24 * Timeline.SECOND;

    private final NA na;
    private final NA gnrsNa;
    private final HashMap<NA, Tuple2<Node, Long>> fib = new HashMap<>();
    private final HashMap<GUID, BiConsumer<? super MFRouter, ? super MFApplicationPacket>> dataConsumers = new HashMap<>();
    private final HashMap<GUID, Tuple3<NA[], Integer, Long>> nrsCache = new HashMap<>();
    private final HashMap<GUID, HashSet<Tuple2<MFApplicationPacket, Node>>> nrsPending = new HashMap<>();

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
    
    public final void setFib(NA na, Tuple2<Node, Long> nextHop) {
        fib.put(na, nextHop);
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

    public final void registerDataConsumer(GUID guid, boolean requestBroadcast, BiConsumer<? super MFRouter, ? super MFApplicationPacket> consumer) {
        BiConsumer<? super MFRouter, ? super MFApplicationPacket> orig = dataConsumers.putIfAbsent(guid, consumer);
        if (orig != null) {
            if (orig == consumer) {
                return;
            }
            throw new IllegalArgumentException("Cannot add 2 consumers to a same data guid");
        }
        //associate
        MFHopPacketGNRSAssociate assoc = new MFHopPacketGNRSAssociate(guid, na, new NA[]{na}, new NA[0], requestBroadcast);
        enqueueIncomingData(this, assoc, true);
    }

    public final void deregisterDataConsumer(GUID guid, boolean requestBroadcast, BiConsumer<? super MFRouter, ? super MFApplicationPacket> consumer) {
        if (dataConsumers.remove(guid, consumer)) {
            MFHopPacketGNRSAssociate assoc = new MFHopPacketGNRSAssociate(guid, na, new NA[0], new NA[]{na}, requestBroadcast);
            enqueueIncomingData(this, assoc, true);
        }
    }

    public final void announceNA() {
        fib.put(na, new Tuple2<>(this, 0L));
        MFHopPacketLSA announce = new MFHopPacketLSA(na, Timeline.nowInUs());
        forEachUnicastLink(l -> sendData(l, announce, true));
    }

    protected Tuple2<Node, Long> getFibEntry(NA na) {
        return fib.get(na);
    }

    protected void sendData(NA target, Data data, boolean prioritized, long delay) {
        if (target == this.na) {
            enqueueIncomingData(this, data, false);
            return;
        }
        Tuple2<Node, Long> nextHop = fib.get(target);
        //next hop should not be null
        if (delay == 0) {
            sendUnicastData(nextHop.getV1(), data, prioritized);
        } else {
            Timeline.addEvent(Timeline.nowInUs() + delay, prams -> {
                sendUnicastData((Node) prams[0], (Data) prams[1], (Boolean) prams[2]);
            }, nextHop.getV1(), data, prioritized);
        }
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
            case MFApplicationPacketData.MF_PACKET_TYPE_DATA:
                return handleMFData(source, (MFApplicationPacketData) p);
        }
        return DURATION_HANDLE_OTHER;
    }

    private boolean updateNRSCacheIfActiveOrPending(GUID guid, NA[] nas, int version) {
        Long now = Timeline.nowInUs();
        Tuple3<NA[], Integer, Long> tuple = nrsCache.get(guid);
//        if (tuple != null // I should have the entry, otherwise, I'll not have the pending
//                && version >= tuple.getV2() // I have it and its version is OK
//                && (tuple.getV3() >= now || nrsPending.containsKey(guid))) { // And, it is active or I have pending
//            tuple.setValues(nas, version, now + durationNrsCacheExpire);
//            return true;
//        }
        if (tuple != null) {
            if (version >= tuple.getV2()) {
                tuple.setValues(nas, version, now + durationNrsCacheExpire);
            }
        } else {
            nrsCache.put(guid, new Tuple3<>(nas, version, now + durationNrsCacheExpire));
        }
        // version is not OK, OR (it is not active and I have no pending)
        return false;
    }

    private boolean forceUpdateNRSCache(GUID guid, NA[] nas, int version) {
        Long now = Timeline.nowInUs();
        Tuple3<NA[], Integer, Long> tuple = nrsCache.get(guid);
        if (tuple == null) { // I don't have it, add
            nrsCache.put(guid, new Tuple3<>(nas, version, now + durationNrsCacheExpire));
            return true;
        }
        if (version >= tuple.getV2()) { // I have it, and the version ok, update
            tuple.setValues(nas, version, now + durationNrsCacheExpire);
            return true;
        }
        return false;
    }

    protected void setLocalNRSCache(GUID guid, NA[] nas) {
        Long now = Timeline.nowInUs();
        Tuple3<NA[], Integer, Long> tuple = nrsCache.get(guid);
        if (tuple == null) { // I don't have it, add
            nrsCache.put(guid, new Tuple3<>(nas, 0, now + durationNrsCacheExpire));
        } else {
            tuple.setValues(nas, tuple.getV2(), now + durationNrsCacheExpire);
        }
    }

    protected void triggerSendApplicationPacket(GUID guid, NA[] nas) {
        if (nas.length == 0) { // no destination bound
            // Here, we don't reassociate, let the caller decide what to do
            return;
        }
        HashSet<Tuple2<MFApplicationPacket, Node>> packets = nrsPending.remove(guid);
        // no pending, do nothing
        if (packets == null) {
            return;
        }
        NA dataDst;
        if (nas.length == 1) { // only 1 NA, use that directly
            dataDst = nas[0];
        } else { // more than 1 NA, find the closest
            dataDst = Stream.of(nas)
                    .map(n -> new Tuple2<>(n, fib.get(n))) // append na with next hop, and distance
                    .min((t1, t2) -> Long.compare(t1.getV2().getV2(), t2.getV2().getV2())) // find the na with minimal distance
                    .map(t -> t.getV1()) // map it back to na
                    .get();
        }
        packets.forEach((packet) -> {
            enqueueIncomingData(packet.getV2(), packet.getV1().copyWithNewDstNa(dataDst), false);
        });
    }

    protected long handleGNRSResponse(Node src, MFHopPacketGNRSResponse packet) {
        long ret = DURATION_HANDLE_NA_FORWARDING;
        NA dst = packet.getNa();
        GUID guid = packet.getGuid();
        NA[] nas = packet.getNas();
        int version = packet.getVersion();

        if (dst == null) {
            // If I have already got the packet before, discard
            if (!packet.addNode(this)) {
                return 0;
            }
            // update if active or pending
            if (updateNRSCacheIfActiveOrPending(guid, nas, version)) {
                ret += DURATION_HANDLE_GNRS_RESPONSE;
                triggerSendApplicationPacket(guid, nas);
            }
            // flood the announcement to other nodes. but how??
            Timeline.addEvent(Timeline.nowInUs() + DURATION_HANDLE_NA_FORWARDING, p -> {
                forEachUnicastLink(l -> Node.sendData(l, (Data) p[0], true));
            }, packet);
        } else {
            if (dst != getNa()) {
                // update if active or pending
                if (updateNRSCacheIfActiveOrPending(guid, nas, version)) {
                    ret += DURATION_HANDLE_GNRS_RESPONSE;
                    triggerSendApplicationPacket(guid, nas);
                }
                // forward packet
                sendData(dst, packet, true, DURATION_HANDLE_GNRS_RESPONSE);
            } else {
                //force update
                if (forceUpdateNRSCache(guid, nas, version)) {
                    ret += DURATION_HANDLE_GNRS_RESPONSE;
                    if (nas.length == 0) {
                        // reissue after a timeout
                        Timeline.addEvent(Timeline.nowInUs() + durationNrsReIssue, this::handleReIssueGNRSRequest, guid);
                    } else {
                        triggerSendApplicationPacket(guid, nas);
                    }
                } else {
                    // reissue after a timeout
                    Timeline.addEvent(Timeline.nowInUs() + durationNrsReIssue, this::handleReIssueGNRSRequest, guid);
                }
            }
        }

        return ret;
    }

    protected void handleReIssueGNRSRequest(Object... params) {
        GUID guid = (GUID) params[0];
        // Check if there is still pending GUID
        if (!nrsPending.containsKey(guid)) {
            return;
        }
        // issue 
        sendData(na, new MFHopPacketGNRSRequest(guid, na), true, 0);
    }

    protected long handleGNRSRequest(Node src, MFHopPacketGNRSRequest packet) {
        sendData(getGnrsNa(), packet, true, DURATION_HANDLE_NA_FORWARDING);
        return DURATION_HANDLE_NA_FORWARDING;
    }

    protected long handleGNRSAssociate(Node src, MFHopPacketGNRSAssociate packet) {
        sendData(getGnrsNa(), packet, true, DURATION_HANDLE_NA_FORWARDING);
        return DURATION_HANDLE_NA_FORWARDING;
    }

    protected long handleMFLSA(Node src, MFHopPacketLSA packet) {
        NA target = packet.getNa();
        if (!fib.containsKey(target)) {
            long duration = Timeline.nowInUs() - packet.getSendTime();
            fib.put(target, new Tuple2<>(src, duration));
            Timeline.addEvent(Timeline.nowInUs() + DURATION_HANDLE_LSA, ps -> {
                forEachUnicastLink(l -> sendData(l, (Data) ps[0], true));
            }, packet);
        }
        return DURATION_HANDLE_LSA;
    }

    protected long handleMFApplication(Node src, MFApplicationPacket packet) {
        if (packet.getDstNA() == null || packet.getDstNA() == this.getNa()) {
            BiConsumer<? super MFRouter, ? super MFApplicationPacket> consumer = dataConsumers.get(packet.getDst());
            if (consumer != null) { // if the application is listening, forward
                Timeline.addEvent(Timeline.nowInUs() + DURATION_HANDLE_DATA_FORWARD_TO_APPLICATION, p
                        -> ((BiConsumer<? super MFRouter, ? super MFApplicationPacket>) p[0])
                                .accept((MFRouter) p[1], (MFApplicationPacket) p[2]),
                        consumer, this, packet);
                return DURATION_HANDLE_DATA_FORWARD_TO_APPLICATION;
            }
        } else { // not sent to me, not first hop either, simply forward
            sendData(packet.getDstNA(), packet, false, DURATION_HANDLE_NA_FORWARDING);
            return DURATION_HANDLE_NA_FORWARDING;
        }
        // now packet.getDstNA is null, try to see what I have in NRS cache
        Tuple3<NA[], Integer, Long> tup = nrsCache.get(packet.getDst());
        if (tup != null && tup.getV3() >= Timeline.nowInUs()) { // has the entry and active
            NA[] nas = tup.getV1();
            NA dataDst;
            if (nas.length == 1) { // only 1 NA, use that directly
                dataDst = nas[0];
                if (dataDst != getNa()) {
                    this.enqueueIncomingData(this, packet.copyWithNewDstNa(dataDst), false);
                    return DURATION_HANDLE_DATA_ADD_NA_AND_FORWARD;
                }
                // otherwise, the result is stale, I should reissue the gnrs request
            } else if (nas.length > 1) { // more than 1 NA, find the closest that is not me
                dataDst = Stream.of(nas).filter(n -> n != getNa())
                        .map(n -> new Tuple2<>(n, fib.get(n))) // append na with next hop, and distance
                        .min((t1, t2) -> Long.compare(t1.getV2().getV2(), t2.getV2().getV2())) // find the na with minimal distance
                        .map(t -> t.getV1()) // map it back to na
                        .get();
                this.enqueueIncomingData(this, packet.copyWithNewDstNa(dataDst), false);
                return DURATION_HANDLE_DATA_ADD_NA_AND_FORWARD;
            }
        }

        // I need to do store and forward
        GUID guid = packet.getDst();
        HashSet<Tuple2<MFApplicationPacket, Node>> tmp = nrsPending.get(guid);
        if (tmp == null) {
            nrsPending.put(guid, tmp = new HashSet<>());
            // I'm the first, issue request
            enqueueIncomingData(this, new MFHopPacketGNRSRequest(guid, na), false);
        }
        tmp.add(new Tuple2<>(packet, src));
        return DURATION_HANDLE_DATA_STORE_AND_FORWARD;
    }

    protected long handleMFData(Node src, MFApplicationPacketData packet) {
        return handleMFApplication(src, packet);
    }

    @Override
    protected void enqueueIncomingData(Node source, Data d, boolean prioritized) {
        super.enqueueIncomingData(source, d, prioritized);
    }

}

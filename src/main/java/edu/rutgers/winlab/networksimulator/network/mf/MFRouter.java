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

    public static final long DURATION_HANDLE_LSA = 100 * Timeline.US;
    public static final long DURATION_HANDLE_NA_FORWARDING = 200 * Timeline.US;
    public static final long DURATION_HANDLE_GNRS_RESPONSE = 1 * Timeline.MS;
    public static final long DURATION_HANDLE_DATA_FORWARD_TO_APPLICATION = 100 * Timeline.US;
    public static final long DURATION_HANDLE_DATA_STORE_AND_FORWARD = 1 * Timeline.MS;
    private static long durationNrsCacheExpire = 5 * Timeline.SECOND;
    private static long durationNrsReIssue = 3 * Timeline.SECOND;

    public static long getDurationNrsCacheExpire() {
        return durationNrsCacheExpire;
    }

    public static void setDurationNrsCacheExpire(long durationNrsCacheExpire) {
        MFRouter.durationNrsCacheExpire = durationNrsCacheExpire;
    }

    public static long getDurationNrsReIssue() {
        return durationNrsReIssue;
    }

    public static void setDurationNrsReIssue(long durationNrsReIssue) {
        MFRouter.durationNrsReIssue = durationNrsReIssue;
    }

    private final NA na;
    private final NA gnrsNa;
    private final HashMap<NA, Tuple2<Node, Long>> fib = new HashMap<>();
    private final HashMap<GUID, BiConsumer<? super MFRouter, ? super MFApplicationPacketData>> dataConsumers = new HashMap<>();
    private final HashMap<GUID, Tuple3<NA[], Integer, Long>> nrsCache = new HashMap<>();
    private final HashMap<GUID, HashSet<MFApplicationPacket>> nrsPending = new HashMap<>();

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

    public final void registerDataConsumer(GUID guid, BiConsumer<? super MFRouter, ? super MFApplicationPacketData> consumer) {
        BiConsumer<? super MFRouter, ? super MFApplicationPacketData> orig = dataConsumers.putIfAbsent(guid, consumer);
        if (orig != null) {
            throw new IllegalArgumentException("Cannot add 2 consumers to a same data guid");
        }
        //associate
        MFHopPacketGNRSAssociate assoc = new MFHopPacketGNRSAssociate(guid, na, new NA[]{na}, new NA[0], false);
        enqueueIncomingData(this, assoc);
    }

    public final void deregisterDataConsumer(GUID guid, BiConsumer<? super MFRouter, ? super MFApplicationPacketData> consumer) {
        if (dataConsumers.remove(guid, consumer)) {
            MFHopPacketGNRSAssociate assoc = new MFHopPacketGNRSAssociate(guid, na, new NA[0], new NA[]{na}, false);
            enqueueIncomingData(this, assoc);
        }
    }

    public final void announceNA() {
        fib.put(na, new Tuple2<>(this, 0L));
        MFHopPacketLSA announce = new MFHopPacketLSA(na, Timeline.nowInUs());
        forEachLink(l -> sendData(l, announce, true));
    }

    protected void sendData(NA target, Data data, boolean prioritized, long delay) {
        if (target == this.na) {
            enqueueIncomingData(this, data);
            return;
        }
        Tuple2<Node, Long> nextHop = fib.get(target);
        //next hop should not be null
        if (delay == 0) {
            sendData(nextHop.getV1(), data, prioritized);
        } else {
            Timeline.addEvent(Timeline.nowInUs() + delay, prams -> {
                sendData((Node) prams[0], (Data) prams[1], (Boolean) prams[2]);
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
        return 0;
    }

    private boolean updateNRSCacheIfActiveOrPending(GUID guid, NA[] nas, int version) {
        Long now = Timeline.nowInUs();
        Tuple3<NA[], Integer, Long> tuple = nrsCache.get(guid);
        if (tuple == null) { // I don't have it, check if there is any pending content
            if (nrsPending.containsKey(guid)) { // I do have pending
                nrsCache.put(guid, new Tuple3<>(nas, version, now + durationNrsCacheExpire));
                return true;
            }
            // I don't have pending, don't update
            return false;
        }
        if (version >= tuple.getV2() // I have it and its version is OK
                && (tuple.getV3() >= now || nrsPending.containsKey(guid))) { // And, it is active or I have pending
            tuple.setValues(nas, version, now + durationNrsCacheExpire);
            return true;
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

    protected void triggerSendApplicationPacket(GUID guid, NA[] nas) {
        if (nas.length == 0) { // no destination bound
            // Here, we don't reassociate, let the caller decide what to do
            return;
        }
        HashSet<MFApplicationPacket> packets = nrsPending.remove(guid);
        // no pending, do nothing
        if (packets == null || packets.isEmpty()) {
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
        packets.stream()
                .map(p -> p.copyWithNewDstNa(dataDst)) // update the dstNA of the data packets
                .forEach(p -> enqueueIncomingData(this, p)); // add to incoming queue
    }

    protected long handleGNRSResponse(Node src, MFHopPacketGNRSResponse packet) {
        long ret = DURATION_HANDLE_NA_FORWARDING;
        NA dst = packet.getNa();
        GUID guid = packet.getGuid();
        NA[] nas = packet.getNas();
        int version = packet.getVersion();

        if (dst == null) {
            // update if active or pending
            if (updateNRSCacheIfActiveOrPending(guid, nas, version)) {
                ret += DURATION_HANDLE_GNRS_RESPONSE;
                triggerSendApplicationPacket(guid, nas);
            }
            // TODO: flood the announcement to other nodes. but how??
            //
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
                forEachLink(l -> sendData(l, (Data) ps[0], true));
            }, packet);
        }
        return DURATION_HANDLE_LSA;
    }

    protected long handleMFData(Node src, MFApplicationPacketData packet) {
        if (packet.getDstNA() == this.getNa()) {
            BiConsumer<? super MFRouter, ? super MFApplicationPacketData> consumer = dataConsumers.get(packet.getDst());
            if (consumer != null) { // if the application is listening, forward
                consumer.accept(this, packet);
                return DURATION_HANDLE_DATA_FORWARD_TO_APPLICATION;
            }
            // clear the dst na
            packet = (MFApplicationPacketData) packet.copyWithNewDstNa(null);
        } else if (packet.getDstNA() != null) { // not sent to me, not first hop either, simply forward
            sendData(packet.getDstNA(), packet, false, DURATION_HANDLE_NA_FORWARDING);
            return DURATION_HANDLE_NA_FORWARDING;
        }
        // now packet.getDstNA is null, I need to do store and forward
        GUID guid = packet.getDst();
        HashSet<MFApplicationPacket> tmp = nrsPending.get(guid);
        if (tmp == null) {
            nrsPending.put(guid, tmp = new HashSet<>());
            // I'm the first, issue request
            enqueueIncomingData(this, new MFHopPacketGNRSRequest(guid, na));
        }
        tmp.add(packet);
        return DURATION_HANDLE_DATA_STORE_AND_FORWARD;
    }

    @Override
    protected void enqueueIncomingData(Node source, Data d) {
        super.enqueueIncomingData(source, d);
    }
    
    
}

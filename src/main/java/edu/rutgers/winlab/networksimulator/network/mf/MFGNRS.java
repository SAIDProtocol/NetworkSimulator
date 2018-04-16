package edu.rutgers.winlab.networksimulator.network.mf;

import edu.rutgers.winlab.networksimulator.common.Data;
import edu.rutgers.winlab.networksimulator.common.PrioritizedQueue;
import edu.rutgers.winlab.networksimulator.common.Timeline;
import edu.rutgers.winlab.networksimulator.common.Tuple2;
import edu.rutgers.winlab.networksimulator.network.Node;
import edu.rutgers.winlab.networksimulator.network.mf.packets.GUID;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFHopPacketGNRSAssociate;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFHopPacketGNRSRequest;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFHopPacketGNRSResponse;
import edu.rutgers.winlab.networksimulator.network.mf.packets.NA;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 *
 * @author Jiachen Chen
 */
public class MFGNRS extends MFRouter {

    public static final long REQUEST_PROCESSING_TIME = 1 * Timeline.MS;
    public static final long ASSOCIATION_PROCESSING_TIME = 1 * Timeline.MS;
    private final HashMap<GUID, Tuple2<HashSet<NA>, Integer>> storage = new HashMap<>();

    public MFGNRS(String name, PrioritizedQueue<Tuple2<Node, Data>> incomingQueue) {
        super(name, incomingQueue);
    }

    public Stream<Entry<GUID, Tuple2<Stream<NA>, Integer>>> getStorageStream() {
        return storage.entrySet().stream().map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), new Tuple2<>(e.getValue().getV1().stream(), e.getValue().getV2())));
    }

    public void forEachStorage(BiConsumer<? super GUID, ? super Tuple2<Stream<NA>, Integer>> consumer) {
        storage.forEach((k, v) -> consumer.accept(k, new Tuple2<>(v.getV1().stream(), v.getV2())));
    }

    public Tuple2<Stream<NA>, Integer> getGuidValue(GUID guid) {
        Tuple2<HashSet<NA>, Integer> ret = storage.get(guid);
        if (ret == null) {
            storage.put(guid, ret = new Tuple2<>(new HashSet<>(), 1));
        }
        return new Tuple2<>(ret.getV1().stream(), ret.getV2());
    }

    @Override
    protected long handleGNRSRequest(Node src, MFHopPacketGNRSRequest packet) {
        GUID guid = packet.getGuid();
        Tuple2<HashSet<NA>, Integer> tmp = storage.get(guid);
        NA[] ret;
        int version;
        if (tmp == null) {
            storage.put(guid, new Tuple2<>(new HashSet<>(), 1));
            ret = new NA[0];
            version = 1;
        } else {
            ret = new NA[tmp.getV1().size()];
            tmp.getV1().toArray(ret);
            version = tmp.getV2();
        }
        MFHopPacketGNRSResponse resp = new MFHopPacketGNRSResponse(guid, packet.getNa(), packet.getServiceId(), ret, version);
        enqueueIncomingData(this, resp);
        return REQUEST_PROCESSING_TIME;

    }

    @Override
    protected long handleGNRSAssociate(Node src, MFHopPacketGNRSAssociate packet) {
        GUID guid = packet.getGuid();
        Tuple2<HashSet<NA>, Integer> tmp = storage.get(guid);
        if (tmp == null) {
            storage.put(guid, tmp = new Tuple2<>(new HashSet<>(), 1));
        }
        HashSet<NA> set = tmp.getV1();
        if (packet.isRemoveExisting()) {
            set.clear();
        }
        set.removeAll(Arrays.asList(packet.getNasRemove()));
        set.addAll(Arrays.asList(packet.getNasAdd()));
        tmp.setV2(tmp.getV2() + 1);
        return ASSOCIATION_PROCESSING_TIME;
    }

}

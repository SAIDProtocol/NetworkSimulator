package edu.rutgers.winlab.networksimulator.network.mf.graphpubsub;

import edu.rutgers.winlab.networksimulator.common.PrioritizedQueue;
import edu.rutgers.winlab.networksimulator.common.QueuePoller;
import edu.rutgers.winlab.networksimulator.common.Timeline;
import edu.rutgers.winlab.networksimulator.common.TriConsumer;
import edu.rutgers.winlab.networksimulator.network.mf.MFRouter;
import edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets.MFApplicationPacketPublication;
import edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets.SerialData;
import edu.rutgers.winlab.networksimulator.network.mf.packets.GUID;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFApplicationPacket;
import edu.rutgers.winlab.networksimulator.network.mf.packets.NA;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 * @author Jiachen Chen
 */
public class MFPubSubRP {

    public static final long DURATION_LOOKUP_NAME_TABLE = 80 * Timeline.US;
    public static final long DURATION_PROCESS_PUBLICATION = 10 * Timeline.US;

    private final QueuePoller<MFApplicationPacketPublication> incomingQueue;
    private final NA na;
    private final Consumer<? super MFApplicationPacketPublication> sender;
    private final TriConsumer<? super GUID, ? super Boolean, BiConsumer<? super MFRouter, ? super MFApplicationPacket>> associator, deAssociator;
    private final HashMap<GUID, HashSet<GUID>> graphTable = new HashMap<>();
    private int incomingPublication = 0, outgoingUnicast = 0, outgoingMulticast = 0;

    public MFPubSubRP(PrioritizedQueue<MFApplicationPacketPublication> queue, NA na,
            Consumer<? super MFApplicationPacketPublication> sender,
            TriConsumer<? super GUID, ? super Boolean, BiConsumer<? super MFRouter, ? super MFApplicationPacket>> associator,
            TriConsumer<? super GUID, ? super Boolean, BiConsumer<? super MFRouter, ? super MFApplicationPacket>> deAssociator) {
        incomingQueue = new QueuePoller<>(this::handlePublication, queue, q -> {
        });
        this.na = na;
        this.sender = sender;
        this.associator = associator;
        this.deAssociator = deAssociator;
    }

    public NA getNa() {
        return na;
    }

    private void addPublicationToQueue(MFRouter router, MFApplicationPacket packet) {
        if (packet instanceof MFApplicationPacketPublication) {
            incomingQueue.enQueue((MFApplicationPacketPublication) packet, false);
        }
    }
    private final BiConsumer<? super MFRouter, ? super MFApplicationPacket> addPublicationToQueueHandler = this::addPublicationToQueue;

    public boolean handleGUID(GUID guid) {
        HashSet<GUID> tmp = graphTable.putIfAbsent(guid, new HashSet<>());
        if (tmp == null) {
            associator.accept(guid, true, addPublicationToQueueHandler);
            return true;
        }
        return false;
    }

    public boolean addGraphRelationship(GUID parent, GUID child) {
        return innerGetGUIDChildren(parent).add(child);
    }

    public boolean removeGraphRelationship(GUID parent, GUID child) {
        return innerGetGUIDChildren(parent).remove(child);
    }

    public Stream<GUID> getGUIDChildren(GUID parent) {
        return innerGetGUIDChildren(parent).stream();
    }

    public void forEachChildGUID(GUID parent, Consumer<? super GUID> consumer) {
        innerGetGUIDChildren(parent).forEach(consumer);
    }

    public HashSet<GUID> removeGUID(GUID guid) {
        HashSet<GUID> tmp = graphTable.remove(guid);
        if (tmp != null) {
            deAssociator.accept(guid, false, addPublicationToQueueHandler);
        }
        return tmp;
    }

    public int getIncomingPublication() {
        return incomingPublication;
    }

    public int getOutgoingUnicast() {
        return outgoingUnicast;
    }

    public int getOutgoingMulticast() {
        return outgoingMulticast;
    }

    public boolean serve(GUID guid) {
        return graphTable.containsKey(guid);
    }

    protected long handlePublication(MFApplicationPacketPublication p) {
//        if (p.getPayload() instanceof SerialData) {
//            SerialData sd = (SerialData) p.getPayload();
//            if (sd.getId() == 300374) {
//                System.out.printf("[%,d] %s RP got pub %d G:NULL(%s)->G:%d(%s) %s%n",
//                        Timeline.nowInUs(), getNa().getNode().getName(), sd.getId(),
//                        p.getSrcNA() == null ? "" : p.getSrcNA().getNode().getName(),
//                        p.getDst().getRepresentation(), p.getDstNA() == null ? "" : p.getDstNA().getNode().getName(),
//                        (p.getPayload() instanceof SerialData) ? ((SerialData) p.getPayload()).getId() + "" : "");
//            }
//        }
//        System.out.printf("[%d] RP %s got G:%d(%s)->G:%d(%s) p:%s%n",
//                Timeline.nowInUs(),
//                na.getNode().getName(),
//                p.getSrc().getRepresentation(),
//                p.getSrcNA() == null ? "NULL" : p.getSrcNA().getNode().getName(),
//                p.getDst().getRepresentation(),
//                p.getDstNA() == null ? "NULL" : p.getDstNA().getNode().getName(),
//                p.getPayload());
        incomingPublication++;
        HashSet<GUID> handle = new HashSet<>(), forward = new HashSet<>();
        long bfsTime = doBFS(p.getDst(), handle, forward);
        outgoingMulticast += handle.size();
        outgoingUnicast += forward.size();
        long timeConsumed = bfsTime + (handle.size() + forward.size()) * DURATION_PROCESS_PUBLICATION;
//        timeConsumed = 0;
        Timeline.addEvent(Timeline.nowInUs() + timeConsumed, ps -> {
            @SuppressWarnings("unchecked")
            HashSet<GUID> h = (HashSet<GUID>) ps[0], f = (HashSet<GUID>) ps[1];
            h.forEach(g -> sender.accept(p.copyWithNewDstGUIDAndSrcNa(g, na)));
            f.forEach(g -> sender.accept(p.copyWithNewDstGUIDAndSrcNa(g, null)));
        }, handle, forward);
        return timeConsumed;
    }

    private HashSet<GUID> innerGetGUIDChildren(GUID parent) {
        HashSet<GUID> tmp = graphTable.get(parent);
        if (tmp == null) {
            throw new IllegalArgumentException("RP does not serve GUID: " + parent.getRepresentation());
        }
        return tmp;
    }

    private long doBFS(GUID start, HashSet<GUID> handle, HashSet<GUID> forward) {
        long ret = 0;
        ArrayDeque<GUID> todo = new ArrayDeque<>();
        todo.push(start);
        GUID current;

        while ((current = todo.poll()) != null) {
            if (!handle.contains(current) && !forward.contains(current)) {
                HashSet<GUID> tmp = graphTable.get(current);
                ret += DURATION_LOOKUP_NAME_TABLE;
                if (tmp == null) {
                    forward.add(current);
                } else {
                    handle.add(current);
                    todo.addAll(tmp);
                }
            }
        }
        return ret;
    }
}

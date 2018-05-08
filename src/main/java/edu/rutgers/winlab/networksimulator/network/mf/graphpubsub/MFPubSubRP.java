package edu.rutgers.winlab.networksimulator.network.mf.graphpubsub;

import edu.rutgers.winlab.networksimulator.common.PrioritizedQueue;
import edu.rutgers.winlab.networksimulator.common.QueuePoller;
import edu.rutgers.winlab.networksimulator.common.TriConsumer;
import edu.rutgers.winlab.networksimulator.network.mf.MFRouter;
import edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets.MFApplicationPacketPublication;
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

    private final QueuePoller<MFApplicationPacketPublication> incomingQueue;
    private final NA na;
    private final Consumer<? super MFApplicationPacketPublication> sender;
    private final TriConsumer<? super GUID, ? super Boolean, BiConsumer<? super MFRouter, ? super MFApplicationPacket>> associator, deAssociator;
    private final HashMap<GUID, HashSet<GUID>> graphTable = new HashMap<>();

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
    
    public boolean serve(GUID guid) {
        return graphTable.containsKey(guid);
    }

    protected long handlePublication(MFApplicationPacketPublication p) {
//        System.out.printf("[%d] RP %s got G:%d(%s)->G:%d(%s) p:%s%n",
//                Timeline.nowInUs(),
//                na.getNode().getName(),
//                p.getSrc().getRepresentation(),
//                p.getSrcNA() == null ? "NULL" : p.getSrcNA().getNode().getName(),
//                p.getDst().getRepresentation(),
//                p.getDstNA() == null ? "NULL" : p.getDstNA().getNode().getName(),
//                p.getPayload());
        HashSet<GUID> handle = new HashSet<>(), forward = new HashSet<>();
        doBFS(p.getDst(), handle, forward);
        handle.forEach(g -> sender.accept(p.copyWithNewDstGUIDAndSrcNa(g, na)));
        forward.forEach(g -> sender.accept(p.copyWithNewDstGUIDAndSrcNa(g, null)));
        return 0;
    }

    private HashSet<GUID> innerGetGUIDChildren(GUID parent) {
        HashSet<GUID> tmp = graphTable.get(parent);
        if (tmp == null) {
            throw new IllegalArgumentException("RP does not serve GUID: " + parent.getRepresentation());
        }
        return tmp;
    }

    private void doBFS(GUID start, HashSet<GUID> handle, HashSet<GUID> forward) {
        ArrayDeque<GUID> todo = new ArrayDeque<>();
        todo.push(start);
        GUID current;

        while ((current = todo.poll()) != null) {
            if (!handle.contains(current) && !forward.contains(current)) {
                HashSet<GUID> tmp = graphTable.get(current);
                if (tmp == null) {
                    forward.add(current);
                } else {
                    handle.add(current);
                    todo.addAll(tmp);
                }
            }
        }
    }
}

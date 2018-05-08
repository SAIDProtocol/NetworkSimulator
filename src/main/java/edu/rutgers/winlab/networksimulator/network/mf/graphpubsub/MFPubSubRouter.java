package edu.rutgers.winlab.networksimulator.network.mf.graphpubsub;

import edu.rutgers.winlab.networksimulator.common.Data;
import edu.rutgers.winlab.networksimulator.common.PrioritizedQueue;
import edu.rutgers.winlab.networksimulator.common.Timeline;
import edu.rutgers.winlab.networksimulator.common.TriConsumer;
import edu.rutgers.winlab.networksimulator.common.Tuple2;
import edu.rutgers.winlab.networksimulator.network.Node;
import edu.rutgers.winlab.networksimulator.network.mf.MFRouter;
import edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets.MFApplicationPacketMark1;
import edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets.MFApplicationPacketMark2;
import edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets.MFApplicationPacketNotifyRP;
import edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets.MFApplicationPacketPublication;
import edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets.MFApplicationPacketSubscription;
import edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets.MFApplicationPacketUnSubscription;
import edu.rutgers.winlab.networksimulator.network.mf.packets.GUID;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFHopPacket;
import edu.rutgers.winlab.networksimulator.network.mf.packets.NA;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 * @author Jiachen Chen
 */
public class MFPubSubRouter extends MFRouter {

    public static final long DURATION_SEND_M1 = 100 * Timeline.MS;
    public static final long DURATION_SEND_M2 = 200 * Timeline.MS;
    public static final long DURATION_LOOKUP_SUBSCRIPTION_TABLE = 2 * Timeline.MS;
    public static final long DURATION_PUBLICATION_MULTICAST = DURATION_LOOKUP_SUBSCRIPTION_TABLE;
    public static final long DURATION_INIT_RP = 2 * Timeline.MS;
    public static final long DURATION_HANDLE_M1 = 1 * Timeline.MS;
    public static final long DURATION_HANDLE_M2 = 2 * Timeline.MS;

    private final HashMap<GUID, HashMap<NA, HashSet<Node>>> subscriptionTable = new HashMap<>();
    private final HashMap<GUID, HashSet<BiConsumer<? super MFPubSubRouter, ? super MFApplicationPacketPublication>>> applications = new HashMap<>();
    private final MFPubSubRP rp;

    public MFPubSubRouter(String name, PrioritizedQueue<Tuple2<Node, Data>> incomingQueue,
            NA gnrsNa, PrioritizedQueue<MFApplicationPacketPublication> rpQueue) {
        super(name, incomingQueue, gnrsNa);
        rp = new MFPubSubRP(rpQueue, getNa(),
                p -> this.enqueueIncomingData(this, p, false),
                this::registerDataConsumer,
                this::deregisterDataConsumer);
    }

    public void forEachSubscriptionTable(TriConsumer<? super GUID, ? super NA, ? super Node> handler) {
        subscriptionTable.forEach((guid, tree) -> {
            tree.forEach((na, nodes) -> {
                nodes.forEach(node -> {
                    handler.accept(guid, na, node);
                });
            });
        });
    }

    public boolean subscribe(GUID guid, BiConsumer<? super MFPubSubRouter, ? super MFApplicationPacketPublication> handler) {
        HashSet<BiConsumer<? super MFPubSubRouter, ? super MFApplicationPacketPublication>> handlers = applications.get(guid);
        if (handlers == null) {
            applications.put(guid, handlers = new HashSet<>());
            handlers.add(handler);
        } else {
            return handlers.add(handler);
        }
        enqueueIncomingData(this, new MFApplicationPacketSubscription(null, guid), true);
        return true;
    }

    public void publish(GUID src, GUID dst, Data payload) {
        enqueueIncomingData(this, new MFApplicationPacketPublication(src, dst, payload), false);
    }

    public boolean registerRP(GUID g) {
        return rp.handleGUID(g);
    }

    public HashSet<GUID> deregisterRP(GUID g) {
        return rp.removeGUID(g);
    }

    public boolean addGraphRelationship(GUID parent, GUID child) {
        return rp.addGraphRelationship(parent, child);
    }

    public boolean removeGraphRelationship(GUID parent, GUID child) {
        return rp.removeGraphRelationship(parent, child);
    }

    public Stream<GUID> getGUIDChildren(GUID parent) {
        return rp.getGUIDChildren(parent);
    }

    public void forEachChildGUID(GUID parent, Consumer<? super GUID> consumer) {
        rp.forEachChildGUID(parent, consumer);
    }

    public void moveRP(GUID guid, NA target) {
        assert rp.serve(guid) : "Can only move rp served at this router!";
        HashSet<GUID> children = rp.removeGUID(guid);
        // update local GNRS by "handleGNRSResponse
        setLocalNRSCache(guid, new NA[]{target});
        // send a notification to target, let it start RP for the guid
        enqueueIncomingData(this, new MFApplicationPacketNotifyRP(guid, children, getNa(), target), true);
        // send subscription to target, subscribe to new RP
        enqueueIncomingData(this, new MFApplicationPacketSubscription(null, guid, target), true);
        // prepare M1 (target)
        Timeline.addEvent(Timeline.nowInUs() + DURATION_SEND_M1, p -> {
            enqueueIncomingData((Node) p[0], (Data) p[1], true);
        }, this, new MFApplicationPacketMark1(guid, getNa(), target));
        // prepare M2 (this NA)
        Timeline.addEvent(Timeline.nowInUs() + DURATION_SEND_M2, p -> {
            MFPubSubRouter r = (MFPubSubRouter) p[0];
            MFApplicationPacketMark2 m2 = (MFApplicationPacketMark2) p[1];
            enqueueIncomingData(r, m2, true);
            // if no subscribers, unsubscribe
            if (!r.hasSubscribers(m2.getGUID(), m2.getDstNA())) {
                enqueueIncomingData((Node) p[0], new MFApplicationPacketUnSubscription(null, m2.getGUID(), m2.getDstNA()), true);
            }
        }, this, new MFApplicationPacketMark2(guid, getNa(), target));
    }

    private boolean hasSubscribers(GUID guid, NA na) {
        if (applications.containsKey(guid)) {
            return true;
        }
        HashMap<NA, HashSet<Node>> tree = subscriptionTable.get(guid);
        if (tree == null) {
            return false;
        }
        HashSet<Node> nextHops = tree.get(na);
        if (nextHops == null) {
            return false;
        }
        if (nextHops.isEmpty()) {
            tree.remove(na);
            if (tree.isEmpty()) {
                subscriptionTable.remove(guid);
            }
            return false;
        }
        return true;
    }

    @Override
    protected long handleData(Tuple2<Node, Data> t) {
        Node source = t.getV1();
        MFHopPacket p = (MFHopPacket) t.getV2();
        switch (p.getType()) {
            case MFApplicationPacketPublication.MF_PACKET_TYPE_PUBLICATION:
                return handlePublication(source, (MFApplicationPacketPublication) p);
            case MFApplicationPacketSubscription.MF_PACKET_TYPE_SUBSCRIPTION:
                return handleSubscription(source, (MFApplicationPacketSubscription) p);
            case MFApplicationPacketNotifyRP.MF_PACKET_TYPE_NOTIFY_RP:
                return handleRPNotification(source, (MFApplicationPacketNotifyRP) p);
            case MFApplicationPacketMark1.MF_PACKET_TYPE_MARK_1:
                return handleMark1(source, (MFApplicationPacketMark1) p);
            case MFApplicationPacketMark2.MF_PACKET_TYPE_MARK_2:
                return handleMark2(source, (MFApplicationPacketMark2) p);
            case MFApplicationPacketUnSubscription.MF_PACKET_TYPE_UNSUBSCRIPTION:
                return handleUnSubscription(source, (MFApplicationPacketUnSubscription) p);
        }
        return super.handleData(t);
    }

    protected long handleMark2(Node src, MFApplicationPacketMark2 packet) {
        GUID guid = packet.getGUID();
        HashMap<NA, HashSet<Node>> tmp = subscriptionTable.get(guid);
        if (tmp != null) {
            HashSet<Node> tmp2 = tmp.get(packet.getSrcNA());
            if (tmp2 != null) {
                tmp2.forEach(n -> sendData(n, packet, true));
                // clean up the subscription table
                tmp.remove(packet.getSrcNA());
                if (tmp.isEmpty()) {
                    subscriptionTable.remove(guid);
                    enqueueIncomingData(this, new MFApplicationPacketUnSubscription(null, guid), true);
                }
            }
        }
        return DURATION_HANDLE_M2;
    }

    protected long handleMark1(Node src, MFApplicationPacketMark1 packet) {
        GUID guid = packet.getGUID();
        if (applications.containsKey(guid)) {
            enqueueIncomingData(this, new MFApplicationPacketSubscription(null, guid, packet.getDstNA()), true);
        }
        HashMap<NA, HashSet<Node>> tmp = subscriptionTable.get(guid);
        if (tmp != null) {
            HashSet<Node> tmp2 = tmp.get(packet.getSrcNA());
            if (tmp2 != null) {
                tmp2.forEach(n -> sendData(n, packet, true));
            }
        }
        return DURATION_HANDLE_M1;
    }

    protected long handleRPNotification(Node src, MFApplicationPacketNotifyRP packet) {
        if (packet.getDstNA() != getNa()) {
            return handleMFApplication(src, packet);
        }
        rp.handleGUID(packet.getGUID());
        packet.getChildren().forEach(child -> rp.addGraphRelationship(packet.getGUID(), child));
        return DURATION_INIT_RP;
    }

    protected long handleUnSubscription(Node src, MFApplicationPacketUnSubscription packet) {
        if (packet.getRPNA() == null) {
            assert src == this : "How did you reach here?";
            // if I'm already subscribing to the name, skip
            if (subscriptionTable.containsKey(packet.getName())) {
                return 0;
            }
            return handleMFApplication(src, packet);
        } else {
            boolean stillSubscribing = false;
            HashMap<NA, HashSet<Node>> tree = subscriptionTable.get(packet.getName());
            if (tree != null) {
                HashSet<Node> nodes = tree.get(packet.getRPNA());
                if (nodes != null) {
                    nodes.remove(src);
                    if (nodes.isEmpty()) {
                        tree.remove(packet.getRPNA());
                        if (tree.isEmpty()) {
                            subscriptionTable.remove(packet.getName());
                        }
                    } else {
                        stillSubscribing = true;
                    }
                }
            }
            if (!stillSubscribing) {
                stillSubscribing = applications.containsKey(packet.getName());
            }

            if (stillSubscribing) {
                return DURATION_LOOKUP_SUBSCRIPTION_TABLE;
            } else {
                return handleMFApplication(src, packet) + DURATION_LOOKUP_SUBSCRIPTION_TABLE;
            }
        }
    }

    protected long handleSubscription(Node src, MFApplicationPacketSubscription packet) {
        if (packet.getRPNA() == null) {
            assert src == this : "How did you reach here?";
            // if I'm already subscribing to the name, skip
            if (subscriptionTable.containsKey(packet.getName())) {
                return 0;
            }
            return handleMFApplication(src, packet);
        } else {
            boolean alreadySubscribed = false;
            HashMap<NA, HashSet<Node>> tree = subscriptionTable.get(packet.getName());
            if (tree == null) {
                subscriptionTable.put(packet.getName(), tree = new HashMap<>());
            }
            HashSet<Node> nodes = tree.get(packet.getRPNA());
            if (nodes != null) {
                alreadySubscribed = true;
            } else {
                tree.put(packet.getRPNA(), nodes = new HashSet<>());
            }
            if (src != this) {
                nodes.add(src);
//                if (!alreadySubscribed) {
//                    alreadySubscribed = applications.containsKey(packet.getName());
//                }
                if (alreadySubscribed) {
                    return DURATION_LOOKUP_SUBSCRIPTION_TABLE;
                } else {
                    return handleMFApplication(src, packet) + DURATION_LOOKUP_SUBSCRIPTION_TABLE;
                }
            } else {
                return handleMFApplication(src, packet) + DURATION_LOOKUP_SUBSCRIPTION_TABLE;
            }

        }
    }

    protected long handlePublication(Node src, MFApplicationPacketPublication packet) {
        // on its way to RP
        if (packet.getSrcNA() == null) {
            return handleMFApplication(src, packet);
        }
        //redundant packet, discard
        if (!packet.addNode(this)) {
            return 0;
        }

        // send it downstream
        GUID dst = packet.getDst();
        HashMap<NA, HashSet<Node>> nextHops = subscriptionTable.get(dst);
        if (nextHops != null) {
            HashSet<Node> targetNodes = new HashSet<>();
            nextHops.values().forEach((value) -> {
                targetNodes.addAll(value);
            });
            targetNodes.forEach(n -> sendData(n, packet, false));
        }
        // send it to applications
        HashSet<BiConsumer<? super MFPubSubRouter, ? super MFApplicationPacketPublication>> tmp = applications.get(packet.getDst());
        if (tmp != null) {
            tmp.forEach(a -> a.accept(MFPubSubRouter.this, packet));
        }
        return DURATION_PUBLICATION_MULTICAST;
    }

}

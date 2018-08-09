package edu.rutgers.winlab.networksimulator.network.mf.graphpubsub;

import edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets.SerialData;
import edu.rutgers.winlab.networksimulator.common.Data;
import edu.rutgers.winlab.networksimulator.common.PrioritizedQueue;
import edu.rutgers.winlab.networksimulator.common.RandomData;
import edu.rutgers.winlab.networksimulator.common.Timeline;
import edu.rutgers.winlab.networksimulator.common.TriConsumer;
import edu.rutgers.winlab.networksimulator.common.Tuple2;
import edu.rutgers.winlab.networksimulator.common.Tuple3;
import edu.rutgers.winlab.networksimulator.common.UnlimitedQueue;
import edu.rutgers.winlab.networksimulator.network.Node;
import edu.rutgers.winlab.networksimulator.network.mf.MFRouter;
import edu.rutgers.winlab.networksimulator.network.mf.MFRouterTest;
import edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets.MFApplicationPacketMark1;
import edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets.MFApplicationPacketMark2;
import edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets.MFApplicationPacketNotifyRP;
import edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets.MFApplicationPacketPublication;
import edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets.MFApplicationPacketSubscription;
import edu.rutgers.winlab.networksimulator.network.mf.packets.GUID;
import edu.rutgers.winlab.networksimulator.network.mf.packets.NA;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Jiachen Chen
 */
public class MFPubSubRouterTest {

    private static final Logger LOG = Logger.getLogger(MFPubSubRouterTest.class.getName());

    public MFPubSubRouterTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    public static void printGNRSEntry(GUID g, Tuple2<Stream<NA>, Integer> t) {
        System.out.printf("GUID:%d NAs:%s ver:%d%n",
                g.getRepresentation(),
                Arrays.toString(t.getV1().map(na -> na.getNode().getName()).toArray()),
                t.getV2()
        );
    }

    public static void printGNRSCacheEntry(GUID g, Tuple3<NA[], Integer, Long> t) {
        System.out.printf("GUID:%d NAs:%s ver:%d exp:%d%n",
                g.getRepresentation(),
                Arrays.toString(Stream.of(t.getV1()).map(na -> na.getNode().getName()).toArray()),
                t.getV2(),
                t.getV3()
        );
    }

    public class ReportingMFPubSubRouter extends MFPubSubRouter {

        public ReportingMFPubSubRouter(String name, PrioritizedQueue<Tuple2<Node, Data>> incomingQueue, NA gnrsNa, PrioritizedQueue<MFApplicationPacketPublication> rpQueue) {
            super(name, incomingQueue, gnrsNa, rpQueue);
        }

        @Override
        protected long handlePublication(Node src, MFApplicationPacketPublication packet) {
            System.out.printf("[%d] %s got pub G:%d(%s)->G:%d(%s) %s%n",
                    Timeline.nowInUs(), getName(),
                    packet.getSrc().getRepresentation(), packet.getSrcNA() == null ? "" : packet.getSrcNA().getNode().getName(),
                    packet.getDst().getRepresentation(), packet.getDstNA() == null ? "" : packet.getDstNA().getNode().getName(),
                    (packet.getPayload() instanceof SerialData) ? ((SerialData) packet.getPayload()).getId() + "" : "");
            return super.handlePublication(src, packet);
        }

        @Override
        protected long handleSubscription(Node src, MFApplicationPacketSubscription packet) {
            System.out.printf("[%d] %s got sub >G:%d(%s) from %s%n",
                    Timeline.nowInUs(), getName(),
                    packet.getDst().getRepresentation(), packet.getDstNA() == null ? "" : packet.getDstNA().getNode().getName(),
                    src.getName());
            return super.handleSubscription(src, packet);
        }

        @Override
        protected long handleRPNotification(Node src, MFApplicationPacketNotifyRP packet) {
            return super.handleRPNotification(src, packet); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        protected long handleMark1(Node src, MFApplicationPacketMark1 packet) {
            return super.handleMark1(src, packet); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        protected long handleMark2(Node src, MFApplicationPacketMark2 packet) {
            return super.handleMark2(src, packet); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void moveRP(GUID guid, NA target) {
            super.moveRP(guid, target); //To change body of generated methods, choose Tools | Templates.
        }

    }

//    @Test
    public void test01() {
        LOG.log(Level.INFO, "RP setup");

        long duration = MFRouter.durationNrsCacheExpire;
        MFRouter.durationNrsCacheExpire = 24 * 60 * 60 * Timeline.SECOND;

        HashMap<String, MFRouter> routers = new HashMap<>();
        MFRouterTest.ReportingMFGNRS gnrs = new MFRouterTest.ReportingMFGNRS("GNRS", new UnlimitedQueue<>());
        routers.put("GNRS", gnrs);
        for (int i = 0; i < 6; i++) {
            String name = "R" + (i + 1);
            routers.put(name, new ReportingMFPubSubRouter(name, new UnlimitedQueue<>(), gnrs.getNa(), new UnlimitedQueue<>()));
        }

        TriConsumer<String, String, Integer> linkNodes = (r1, r2, latency)
                -> Node.linkNodes(routers.get(r1), routers.get(r2), 100 * Node.BW_IN_MBPS, latency * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());

        linkNodes.accept("R1", "R2", 7);
        linkNodes.accept("R1", "R3", 9);
        linkNodes.accept("R1", "R6", 14);
        linkNodes.accept("R2", "R3", 10);
        linkNodes.accept("R2", "R4", 15);
        linkNodes.accept("R3", "R4", 11);
        linkNodes.accept("R3", "R6", 2);
        linkNodes.accept("R4", "R5", 6);
        linkNodes.accept("R5", "R6", 9);
        linkNodes.accept("R5", "GNRS", 1);

        routers.values().forEach(router -> {
            Timeline.addEvent(0, p -> router.announceNA());
            Timeline.run();
        });

        Timeline.addEvent(0, p -> {
            ((MFPubSubRouter) routers.get("R5")).registerRP(new GUID(101));
            ((MFPubSubRouter) routers.get("R5")).registerRP(new GUID(101));
            ((MFPubSubRouter) routers.get("R5")).registerRP(new GUID(100));
            ((MFPubSubRouter) routers.get("R5")).registerRP(new GUID(98));
            ((MFPubSubRouter) routers.get("R5")).registerRP(new GUID(97));
            ((MFPubSubRouter) routers.get("R5")).addGraphRelationship(new GUID(101), new GUID(100));
            ((MFPubSubRouter) routers.get("R5")).addGraphRelationship(new GUID(101), new GUID(99));
            ((MFPubSubRouter) routers.get("R5")).addGraphRelationship(new GUID(100), new GUID(99));
            ((MFPubSubRouter) routers.get("R5")).addGraphRelationship(new GUID(100), new GUID(98));
            ((MFPubSubRouter) routers.get("R5")).addGraphRelationship(new GUID(97), new GUID(98));
            ((MFPubSubRouter) routers.get("R5")).addGraphRelationship(new GUID(98), new GUID(97));
            ((MFPubSubRouter) routers.get("R3")).registerRP(new GUID(99));
            ((MFPubSubRouter) routers.get("R3")).addGraphRelationship(new GUID(99), new GUID(98));
        });
        Timeline.run();

        gnrs.forEachStorage(MFPubSubRouterTest::printGNRSEntry);

        BiConsumer<MFPubSubRouter, MFApplicationPacketPublication> publicationHandler = (r, p) -> {
            System.out.printf("[%d] %s>APP got pub G:%d->G:%d%n", Timeline.nowInUs(), r.getName(), p.getSrc().getRepresentation(), p.getDst().getRepresentation());
        };

        Timeline.addEvent(0, p -> {
            ((MFPubSubRouter) routers.get("R1")).subscribe(new GUID(101), publicationHandler);
            ((MFPubSubRouter) routers.get("R1")).subscribe(new GUID(100), publicationHandler);
            ((MFPubSubRouter) routers.get("R2")).subscribe(new GUID(99), publicationHandler);
            ((MFPubSubRouter) routers.get("R2")).subscribe(new GUID(98), publicationHandler);
            ((MFPubSubRouter) routers.get("R1")).subscribe(new GUID(97), publicationHandler);
        });
        Timeline.run();

        routers.values().forEach(router -> {
            if (router instanceof MFPubSubRouter) {
                System.out.printf("====ST of %s====%n", router.getName());
                ((MFPubSubRouter) router).forEachSubscriptionTable((guid, na, node) -> {
                    System.out.printf("G:%d(%s)->%s%n", guid.getRepresentation(), na.getNode().getName(), node.getName());
                });
            }
        });

        Timeline.addEvent(0, p -> ((MFPubSubRouter) routers.get("R4")).publish(new GUID(90), new GUID(101), new RandomData(1000 * Data.BYTE)));
        Timeline.addEvent(1 * Timeline.SECOND, p -> ((MFPubSubRouter) routers.get("R4")).publish(new GUID(90), new GUID(101), new RandomData(1000 * Data.BYTE)));
        Timeline.addEvent(6 * Timeline.SECOND, p -> ((MFPubSubRouter) routers.get("R4")).publish(new GUID(90), new GUID(101), new RandomData(1000 * Data.BYTE)));

        Timeline.addEvent(8 * Timeline.SECOND, p -> {
            ((MFPubSubRouter) routers.get("R5")).moveRP(new GUID(100), ((MFPubSubRouter) routers.get("R2")).getNa());
            ((MFPubSubRouter) routers.get("R5")).publish(new GUID(90), new GUID(100), new RandomData(1000 * Data.BYTE));
//            GUID toMove = new GUID(100);
//            HashSet<GUID> children = ((MFPubSubRouter) routers.get("R5")).deregisterRP(toMove);
//            ((MFPubSubRouter) routers.get("R3")).registerRP(toMove);
//            gnrs.handleGNRSAssociate(gnrs, new MFHopPacketGNRSAssociate(toMove, gnrs.getNa(), new NA[]{routers.get("R3").getNa()}, new NA[]{routers.get("R5").getNa()}, true));

//            routers.values().forEach(r -> {
//                if (r instanceof MFPubSubRouter) {
//                    ((MFPubSubRouter) r).magicallyClearSubscrption(toMove);
//                }
//            });
//            routers.values().forEach(r -> {
//                if (r instanceof MFPubSubRouter) {
//                    ((MFPubSubRouter) r).magicallyPropagateSubscription(toMove);
//                }
//            });
//            gnrs.forEachStorage(MFPubSubRouterTest::printGNRSEntry);
//            routers.values().forEach(r -> {
//                if (r instanceof MFPubSubRouter) {
//                    System.out.printf("====ST of %s====%n", r.getName());
//                    ((MFPubSubRouter) r).forEachSubscriptionTable((g, s) -> {
//                        System.out.printf("G:%d->%s%n", g.getRepresentation(), Arrays.toString(s.map(n -> n.getName()).toArray()));
//                    });
//                }
//            });
//            children.forEach(g -> ((MFPubSubRouter) routers.get("R3")).addGraphRelationship(toMove, g));
        });
//        Timeline.addEvent(8 * Timeline.SECOND, p -> ((MFPubSubRouter) routers.get("R4")).publish(new GUID(90), new GUID(101), new RandomData(1000 * Data.BYTE)));

        Timeline.run();

        routers.values().forEach(r -> {
            System.out.printf("====GNRS cache on %s====%n", r.getName());
            r.forEachNrsCache(MFPubSubRouterTest::printGNRSCacheEntry);
        });

        System.out.printf("====GNRS====%n");
        gnrs.forEachStorage((g, t) -> System.out.printf("G:%d NAs:%s ver:%d%n", g.getRepresentation(), Arrays.toString(t.getV1().map(na -> na.getNode().getName()).toArray()), t.getV2()));

        routers.values().forEach(router -> {
            if (router instanceof MFPubSubRouter) {
                System.out.printf("====ST of %s====%n", router.getName());
                ((MFPubSubRouter) router).forEachSubscriptionTable((guid, na, node) -> {
                    System.out.printf("G:%d(%s)->%s%n", guid.getRepresentation(), na.getNode().getName(), node.getName());
                });
            }
        });

        MFRouter.durationNrsCacheExpire = duration;
    }

//    @Test
    public void test02() {
        LOG.log(Level.INFO, "RP migration");

        long duration = MFRouter.durationNrsCacheExpire;
        MFRouter.durationNrsCacheExpire = 24 * 60 * 60 * Timeline.SECOND;

        HashMap<String, MFPubSubRouter> routers = new HashMap<>();
        MFRouterTest.ReportingMFGNRS gnrs = new MFRouterTest.ReportingMFGNRS("GNRS", new UnlimitedQueue<>());
        for (int i = 0; i < 6; i++) {
            String name = "R" + (i + 1);
            routers.put(name, new ReportingMFPubSubRouter(name, new UnlimitedQueue<>(), gnrs.getNa(), new UnlimitedQueue<>()));
        }

        TriConsumer<String, String, Integer> linkNodes = (r1, r2, latency)
                -> Node.linkNodes(routers.get(r1), routers.get(r2), 100 * Node.BW_IN_MBPS, latency * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());

        linkNodes.accept("R1", "R2", 7);
        linkNodes.accept("R1", "R3", 9);
        linkNodes.accept("R1", "R6", 14);
        linkNodes.accept("R2", "R3", 10);
        linkNodes.accept("R2", "R4", 15);
        linkNodes.accept("R3", "R4", 11);
        linkNodes.accept("R3", "R6", 2);
        linkNodes.accept("R4", "R5", 6);
        linkNodes.accept("R5", "R6", 9);
        Node.linkNodes(routers.get("R5"), gnrs, 100 * Node.BW_IN_MBPS, 1 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());

        Timeline.addEvent(0, p -> gnrs.announceNA());
        Timeline.run();
        routers.values().forEach(router -> {
            Timeline.addEvent(0, p -> router.announceNA());
            Timeline.run();
        });

        Timeline.addEvent(0, p -> {
            (routers.get("R3")).registerRP(new GUID(99));
            (routers.get("R5")).registerRP(new GUID(100));
            (routers.get("R5")).addGraphRelationship(new GUID(100), new GUID(99));
        });
        Timeline.run();

        int packetCount = 100;
        long packetGap = 30 * Timeline.MS;

        HashMap<Node, Tuple2<HashSet<Integer>, Integer>> node100Results = new HashMap<>(), node99Results = new HashMap<>();
        HashMap<Integer, ArrayList<Long>> packet100Results = new HashMap<>(), packet99Results = new HashMap<>();

        routers.values().forEach((value) -> {
            node100Results.put(value, new Tuple2<>(new HashSet<>(), 0));
            node99Results.put(value, new Tuple2<>(new HashSet<>(), 0));
        });
        for (int i = 0; i < packetCount; i++) {
            packet100Results.put(i, new ArrayList<>());
            packet99Results.put(i, new ArrayList<>());
        }

        BiConsumer<MFPubSubRouter, MFApplicationPacketPublication> publicationHandler = (r, p) -> {
            HashMap<Node, Tuple2<HashSet<Integer>, Integer>> nodeResults;
            HashMap<Integer, ArrayList<Long>> packetResults;
            if (p.getDst().getRepresentation() == 100) {
                nodeResults = node100Results;
                packetResults = packet100Results;
            } else {
                nodeResults = node99Results;
                packetResults = packet99Results;
            }
            int serial = ((SerialData) p.getPayload()).getId();
            Tuple2<HashSet<Integer>, Integer> t = nodeResults.get(r);
            t.setV2(t.getV2() + 1);
            if (t.getV1().add(serial)) {
                packetResults.get(serial).add(Timeline.nowInUs());
            }
        };

        Timeline.addEvent(0, p -> {
//            routers.get("R5").subscribe(new GUID(99), publicationHandler);
//            routers.get("R6").subscribe(new GUID(99), publicationHandler);
            routers.values().forEach(r -> {
                r.subscribe(new GUID(100), publicationHandler);
                r.subscribe(new GUID(99), publicationHandler);
            });
        });
        Timeline.run();
        routers.values().forEach(router -> {
            System.out.printf("====ST of %s====%n", router.getName());
            ((MFPubSubRouter) router).forEachSubscriptionTable((guid, na, node) -> {
                System.out.printf("G:%d(%s)->%s%n", guid.getRepresentation(), na.getNode().getName(), node.getName());
            });
        });

        for (int i = 0; i < packetCount; i++) {
            Timeline.addEvent(i * packetGap, p -> routers.get("R1").publish(new GUID(101), new GUID(100), new SerialData((int) p[0], 1000 * Data.BYTE)), i);
        }
        Timeline.addEvent(1 * Timeline.SECOND, p -> routers.get("R5").moveRP(new GUID(100), routers.get("R2").getNa()));

        Timeline.run();
        routers.values().forEach((value) -> {
            System.out.printf("%s: %d/%d %d/%d%n", value.getName(),
                    node99Results.get(value).getV2(), node99Results.get(value).getV1().size(),
                    node100Results.get(value).getV2(), node100Results.get(value).getV1().size());
        });
        for (int i = 0; i < packetCount; i++) {
            int size99 = packet99Results.get(i).size();
            long sum99 = packet99Results.get(i).stream().reduce(Long::sum).get();
            double avg99 = (double) sum99 / size99 - i * packetGap;
            int size100 = packet100Results.get(i).size();
            long sum100 = packet100Results.get(i).stream().reduce(Long::sum).get();
            double avg100 = (double) sum100 / size100 - i * packetGap;
            System.out.printf("%d: %d %f %d %f%n", i,
                    size99, avg99 / Timeline.MS, size100, avg100 / Timeline.MS);
        }

        MFRouter.durationNrsCacheExpire = duration;
    }

}

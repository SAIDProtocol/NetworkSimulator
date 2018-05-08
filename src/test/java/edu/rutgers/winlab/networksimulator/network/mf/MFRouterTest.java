package edu.rutgers.winlab.networksimulator.network.mf;

import edu.rutgers.winlab.networksimulator.common.Data;
import static edu.rutgers.winlab.networksimulator.common.Helper.assertStreamEquals;
import edu.rutgers.winlab.networksimulator.common.PrioritizedQueue;
import edu.rutgers.winlab.networksimulator.common.RandomData;
import edu.rutgers.winlab.networksimulator.common.Timeline;
import edu.rutgers.winlab.networksimulator.common.TriConsumer;
import edu.rutgers.winlab.networksimulator.common.Tuple2;
import edu.rutgers.winlab.networksimulator.common.UnlimitedQueue;
import edu.rutgers.winlab.networksimulator.network.Node;
import edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.MFPubSubRouterTest;
import edu.rutgers.winlab.networksimulator.network.mf.packets.GUID;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFApplicationPacket;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFApplicationPacketData;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFHopPacket;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFHopPacketGNRSAssociate;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFHopPacketGNRSRequest;
import edu.rutgers.winlab.networksimulator.network.mf.packets.NA;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jiachen
 */
public class MFRouterTest {

    private static final Logger LOG = Logger.getLogger(MFRouterTest.class.getName());

    public MFRouterTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Test
    public void test1() {
        LOG.log(Level.INFO, "Routing test");
        HashMap<String, MFRouter> routers = new HashMap<>();
        MFGNRS gnrs = new MFGNRS("R5", new UnlimitedQueue<>());
        routers.put("R5", gnrs);
        for (int i = 0; i < 6; i++) {
            if (i != 4) {
                String name = "R" + (i + 1);
                routers.put(name, new MFRouter(name, new UnlimitedQueue<>(), gnrs.getNa()));
            }
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

        routers.values().forEach(router -> {
            Timeline.addEvent(0, p -> router.announceNA());
            Timeline.run();
        });

        HashMap<Node, HashMap<NA, Node>> target = new HashMap<>();
        HashMap<NA, Node> tmp;
        target.put(routers.get("R1"), tmp = new HashMap<>());
        tmp.put(routers.get("R1").getNa(), routers.get("R1"));
        tmp.put(routers.get("R2").getNa(), routers.get("R2"));
        tmp.put(routers.get("R3").getNa(), routers.get("R3"));
        tmp.put(routers.get("R4").getNa(), routers.get("R3"));
        tmp.put(routers.get("R5").getNa(), routers.get("R3"));
        tmp.put(routers.get("R6").getNa(), routers.get("R3"));
        target.put(routers.get("R2"), tmp = new HashMap<>());
        tmp.put(routers.get("R1").getNa(), routers.get("R1"));
        tmp.put(routers.get("R2").getNa(), routers.get("R2"));
        tmp.put(routers.get("R3").getNa(), routers.get("R3"));
        tmp.put(routers.get("R4").getNa(), routers.get("R4"));
        tmp.put(routers.get("R5").getNa(), routers.get("R4"));
        tmp.put(routers.get("R6").getNa(), routers.get("R3"));
        target.put(routers.get("R3"), tmp = new HashMap<>());
        tmp.put(routers.get("R1").getNa(), routers.get("R1"));
        tmp.put(routers.get("R2").getNa(), routers.get("R2"));
        tmp.put(routers.get("R3").getNa(), routers.get("R3"));
        tmp.put(routers.get("R4").getNa(), routers.get("R4"));
        tmp.put(routers.get("R5").getNa(), routers.get("R6"));
        tmp.put(routers.get("R6").getNa(), routers.get("R6"));
        target.put(routers.get("R4"), tmp = new HashMap<>());
        tmp.put(routers.get("R1").getNa(), routers.get("R3"));
        tmp.put(routers.get("R2").getNa(), routers.get("R2"));
        tmp.put(routers.get("R3").getNa(), routers.get("R3"));
        tmp.put(routers.get("R4").getNa(), routers.get("R4"));
        tmp.put(routers.get("R5").getNa(), routers.get("R5"));
        tmp.put(routers.get("R6").getNa(), routers.get("R3"));
        target.put(routers.get("R5"), tmp = new HashMap<>());
        tmp.put(routers.get("R1").getNa(), routers.get("R6"));
        tmp.put(routers.get("R2").getNa(), routers.get("R4"));
        tmp.put(routers.get("R3").getNa(), routers.get("R6"));
        tmp.put(routers.get("R4").getNa(), routers.get("R4"));
        tmp.put(routers.get("R5").getNa(), routers.get("R5"));
        tmp.put(routers.get("R6").getNa(), routers.get("R6"));
        target.put(routers.get("R6"), tmp = new HashMap<>());
        tmp.put(routers.get("R1").getNa(), routers.get("R3"));
        tmp.put(routers.get("R2").getNa(), routers.get("R3"));
        tmp.put(routers.get("R3").getNa(), routers.get("R3"));
        tmp.put(routers.get("R4").getNa(), routers.get("R3"));
        tmp.put(routers.get("R5").getNa(), routers.get("R5"));
        tmp.put(routers.get("R6").getNa(), routers.get("R6"));

//        routers.values().forEach(router -> {
//            LOG.log(Level.INFO, "FIB of router {0}", router.getName());
//            router.forEachFib((na, tup)
//                    -> LOG.log(Level.INFO, "->{0} {1} ({2})", new Object[]{na.getNode().getName(), tup.getV1().getName(), tup.getV2()}));
//        });
        routers.values().forEach(router -> {
            HashMap<NA, Node> t = target.get(router);
            if (t != null) {
                assertTrue(router.fibStream().allMatch(e -> t.remove(e.getKey(), e.getValue().getV1())));
                t.forEach((na, node) -> {
                    fail(String.format("Missing match %s->%s:%s", router.getName(), na.getNode().getName(), node.getName()));
                });
            }
        });
        routers.values().forEach(router -> router.clearFib());
        routers.values().stream().allMatch(r -> r.fibStream().count() == 0);
    }

    @Test
    public void test2() {
        LOG.log(Level.INFO, "GNRS test");
        HashMap<String, MFRouter> routers = new HashMap<>();
        MFGNRS gnrs = new MFGNRS("R5", new UnlimitedQueue<>());
        routers.put("R5", gnrs);
        for (int i = 0; i < 6; i++) {
            if (i != 4) {
                String name = "R" + (i + 1);
                routers.put(name, new MFRouter(name, new UnlimitedQueue<>(), gnrs.getNa()));
            }
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

        routers.values().forEach(router -> {
            Timeline.addEvent(0, p -> router.announceNA());
            Timeline.run();
        });

        BiConsumer<Node, MFApplicationPacket> consumer1 = (n, d) -> {
            System.out.printf("%s Got: %s%n", n.getName(), d);
        };

        int gargetGUID = 100;
        Tuple2<Stream<NA>, Integer> ret;
        NA na1 = routers.get("R1").getNa();
        NA na3 = routers.get("R3").getNa();

        ret = gnrs.getGuidValue(new GUID(gargetGUID - 1));
        assertStreamEquals(Stream.empty(), ret.getV1());
        assertEquals(1, (int) ret.getV2());

        routers.get("R1").registerDataConsumer(new GUID(gargetGUID), false, consumer1);
        Timeline.run();
        ret = gnrs.getGuidValue(new GUID(gargetGUID));
        assertStreamEquals(Stream.of(na1), ret.getV1());
        assertEquals(2, (int) ret.getV2());

        routers.get("R3").registerDataConsumer(new GUID(gargetGUID), false, consumer1);
        Timeline.run();
        ret = gnrs.getGuidValue(new GUID(gargetGUID));
        assertStreamEquals(Stream.of(na1, na3), ret.getV1().sorted());
        assertEquals(3, (int) ret.getV2());

        routers.get("R3").deregisterDataConsumer(new GUID(gargetGUID), false, consumer1);
        Timeline.run();
        ret = gnrs.getGuidValue(new GUID(gargetGUID));
        assertStreamEquals(Stream.of(na1), ret.getV1().sorted());
        assertEquals(4, (int) ret.getV2());

        routers.get("R1").deregisterDataConsumer(new GUID(gargetGUID), false, consumer1);
        Timeline.run();
        ret = gnrs.getGuidValue(new GUID(gargetGUID));
        assertStreamEquals(Stream.empty(), ret.getV1().sorted());
        assertEquals(5, (int) ret.getV2());

        routers.get("R1").deregisterDataConsumer(new GUID(gargetGUID - 1), false, consumer1);
        Timeline.run();
        ret = gnrs.getGuidValue(new GUID(gargetGUID));
        assertStreamEquals(Stream.empty(), ret.getV1().sorted());
        assertEquals(5, (int) ret.getV2());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test3() {
        MFRouter r = new MFRouter("Test", new UnlimitedQueue<>());
        fail("Should not reach here!");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test4() {
        MFRouter r = new MFRouter("Test", new UnlimitedQueue<>(), null);
        fail("Should not reach here!");
    }

    @Test()
    public void test5() {
        MFGNRS gnrs = new MFGNRS("GNRS", new UnlimitedQueue<>());
        MFRouter r = new MFRouter("R1", new UnlimitedQueue<>(), gnrs.getNa());
        try {
            MFRouter r2 = new MFRouter("R2", new UnlimitedQueue<>(), r.getNa());
            fail("Should not reach here");
        } catch (IllegalArgumentException e) {

        }
        MFRouter r3 = new MFRouter("R2", new UnlimitedQueue<>(), r.getGnrsNa());

        Timeline.addEvent(0, p -> {
            r3.enqueueIncomingData(r3, new MFHopPacket(-100) {
                @Override
                public int getSizeInBits() {
                    return 0;
                }
            }, false);
        });
        Timeline.run();
    }

    public static class ReportingMFGNRS extends MFGNRS {

        public ReportingMFGNRS(String name, PrioritizedQueue<Tuple2<Node, Data>> incomingQueue) {
            super(name, incomingQueue);
        }

        @Override
        public long handleGNRSAssociate(Node src, MFHopPacketGNRSAssociate packet) {
            long duration = super.handleGNRSAssociate(src, packet);
            Tuple2<Stream<NA>, Integer> res = super.getGuidValue(packet.getGuid());
            System.out.printf("[%d] Assoc GUID:%d, NA:%s ADD:%s REM:%s ver:%d, res:%s%n",
                    Timeline.nowInUs(),
                    packet.getGuid().getRepresentation(),
                    packet.getNa().getNode().getName(),
                    Arrays.toString(Stream.of(packet.getNasAdd()).map(na -> na.getNode().getName()).toArray()),
                    Arrays.toString(Stream.of(packet.getNasRemove()).map(na -> na.getNode().getName()).toArray()),
                    res.getV2(),
                    Arrays.toString(res.getV1().map(na -> na.getNode().getName()).toArray()));
            return duration;
        }

        @Override
        protected long handleGNRSRequest(Node src, MFHopPacketGNRSRequest packet) {
            long duration = super.handleGNRSRequest(src, packet);
            Tuple2<Stream<NA>, Integer> res = super.getGuidValue(packet.getGuid());
            System.out.printf("[%d] REQ GUID:%d, NA:%s ver:%d, res:%s%n",
                    Timeline.nowInUs(),
                    packet.getGuid().getRepresentation(),
                    packet.getNa().getNode().getName(),
                    res.getV2(),
                    Arrays.toString(res.getV1().map(na -> na.getNode().getName()).toArray()));
            return duration;
        }

        public void playWithVersion(GUID guid, int versionAdd) {
            Tuple2<HashSet<NA>, Integer> entry = getStorage().get(guid);
            entry.setV2(entry.getV2() + versionAdd);
        }

    }

    @Test
    public void test6() {
        LOG.log(Level.INFO, "Basic store and forward");

        MFGNRS gnrs = new ReportingMFGNRS("GNRS", new UnlimitedQueue<>());
        MFRouter r1 = new MFRouter("R1", new UnlimitedQueue<>(), gnrs.getNa());
        MFRouter r2 = new MFRouter("R2", new UnlimitedQueue<>(), gnrs.getNa());

        MFRouter[] nodes = new MFRouter[]{gnrs, r1, r2};

        Node.linkNodes(r1, gnrs, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(r2, gnrs, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());

        for (MFRouter node : nodes) {
            Timeline.addEvent(0, p -> ((MFRouter) p[0]).announceNA(), node);
            Timeline.run();
        }

        Timeline.addEvent(0, p -> r1.enqueueIncomingData(r1, new MFApplicationPacketData(new GUID(100), new GUID(101), new RandomData(1000 * Data.BYTE)), false));

        BiConsumer<MFRouter, MFApplicationPacket> dataConsumer = (r, d) -> {
            System.out.printf("[%d] %s received G:%d->G:%d, len=%d%n",
                    Timeline.nowInUs(),
                    r.getName(),
                    d.getSrc().getRepresentation(),
                    d.getDst().getRepresentation(),
                    d.getSizeInBits() / Data.BYTE);
        };

        BiConsumer<MFRouter, MFApplicationPacket> c2 = (r, d) -> {
        };

        Timeline.addEvent(15 * Timeline.SECOND, p -> r2.registerDataConsumer(new GUID(101), false, dataConsumer));
        Timeline.addEvent(16 * Timeline.SECOND, p -> {
            try {
                r2.registerDataConsumer(new GUID(101), false, c2);
                fail("Should not reach here");
            } catch (IllegalArgumentException e) {

            }
            r2.registerDataConsumer(new GUID(101), false, dataConsumer);
        });

        Timeline.run();

        //TODO add verify
    }

    @Test
    public void test7() {
        LOG.log(Level.INFO, "Basic anycast");

        MFGNRS gnrs = new ReportingMFGNRS("GNRS", new UnlimitedQueue<>());
        MFRouter r1 = new MFRouter("R1", new UnlimitedQueue<>(), gnrs.getNa());
        MFRouter r2 = new MFRouter("R2", new UnlimitedQueue<>(), gnrs.getNa());
        MFRouter r3 = new MFRouter("R3", new UnlimitedQueue<>(), gnrs.getNa());

        MFRouter[] nodes = new MFRouter[]{gnrs, r1, r2, r3};

        Node.linkNodes(r1, gnrs, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(r2, gnrs, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(r3, gnrs, 100 * Node.BW_IN_MBPS, 50 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());

        for (MFRouter node : nodes) {
            Timeline.addEvent(0, p -> ((MFRouter) p[0]).announceNA(), node);
            Timeline.run();
        }

        Timeline.addEvent(0, p -> r1.enqueueIncomingData(r1, new MFApplicationPacketData(new GUID(100), new GUID(101), new RandomData(1000 * Data.BYTE)), false));

        BiConsumer<MFRouter, MFApplicationPacket> dataConsumer = (r, d) -> {
            System.out.printf("[%d] %s received G:%d->G:%d, len=%d%n",
                    Timeline.nowInUs(),
                    r.getName(),
                    d.getSrc().getRepresentation(),
                    d.getDst().getRepresentation(),
                    d.getSizeInBits() / Data.BYTE);
        };

        Timeline.addEvent(15 * Timeline.SECOND, p -> r2.registerDataConsumer(new GUID(101), false, dataConsumer));
        Timeline.addEvent(15 * Timeline.SECOND, p -> r3.registerDataConsumer(new GUID(101), false, dataConsumer));

        Timeline.run();

        //TODO add verify
    }

    @Test
    public void test8() {
        LOG.log(Level.INFO, "Basic mobility");

        MFGNRS gnrs = new ReportingMFGNRS("GNRS", new UnlimitedQueue<>());
        MFRouter r1 = new MFRouter("R1", new UnlimitedQueue<>(), gnrs.getNa());
        MFRouter r2 = new MFRouter("R2", new UnlimitedQueue<>(), gnrs.getNa());
        MFRouter r3 = new MFRouter("R3", new UnlimitedQueue<>(), gnrs.getNa());

        MFRouter[] nodes = new MFRouter[]{gnrs, r1, r2, r3};

        Node.linkNodes(r1, gnrs, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(r2, gnrs, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(r3, gnrs, 100 * Node.BW_IN_MBPS, 50 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());

        for (MFRouter node : nodes) {
            Timeline.addEvent(0, p -> ((MFRouter) p[0]).announceNA(), node);
            Timeline.run();
        }

        Timeline.addEvent(0, p -> r1.enqueueIncomingData(r1, new MFApplicationPacketData(new GUID(100), new GUID(101), new RandomData(1000 * Data.BYTE)), false));

        BiConsumer<MFRouter, MFApplicationPacket> dataConsumer = (r, d) -> {
            System.out.printf("[%d] %s received G:%d->G:%d, len=%d%n",
                    Timeline.nowInUs(),
                    r.getName(),
                    d.getSrc().getRepresentation(),
                    d.getDst().getRepresentation(),
                    d.getSizeInBits() / Data.BYTE);
        };

        Timeline.addEvent(15 * Timeline.SECOND, p -> r2.registerDataConsumer(new GUID(101), false, dataConsumer));
        Timeline.addEvent(16113 * Timeline.MS, p -> r2.deregisterDataConsumer(new GUID(101), false, dataConsumer));
        Timeline.addEvent(20 * Timeline.SECOND, p -> r3.registerDataConsumer(new GUID(101), false, dataConsumer));
        Timeline.run();

        //TODO add verify
    }

    @Test
    public void test9() {
        LOG.log(Level.INFO, "Version Error");

        ReportingMFGNRS gnrs = new ReportingMFGNRS("GNRS", new UnlimitedQueue<>());
        MFRouter r1 = new MFRouter("R1", new UnlimitedQueue<>(), gnrs.getNa());
        MFRouter r2 = new MFRouter("R2", new UnlimitedQueue<>(), gnrs.getNa());
        MFRouter r3 = new MFRouter("R3", new UnlimitedQueue<>(), gnrs.getNa());

        MFRouter[] nodes = new MFRouter[]{gnrs, r1, r2, r3};

        Node.linkNodes(r1, gnrs, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(r2, gnrs, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(r3, gnrs, 100 * Node.BW_IN_MBPS, 50 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());

        for (MFRouter node : nodes) {
            Timeline.addEvent(0, p -> ((MFRouter) p[0]).announceNA(), node);
            Timeline.run();
        }

        Timeline.addEvent(0, p -> r1.enqueueIncomingData(r1, new MFApplicationPacketData(new GUID(100), new GUID(101), new RandomData(1000 * Data.BYTE)), false));

        BiConsumer<MFRouter, MFApplicationPacket> dataConsumer = (r, d) -> {
            System.out.printf("[%d] %s received G:%d->G:%d, len=%d%n",
                    Timeline.nowInUs(),
                    r.getName(),
                    d.getSrc().getRepresentation(),
                    d.getDst().getRepresentation(),
                    d.getSizeInBits() / Data.BYTE);
        };
        Timeline.addEvent(9 * Timeline.SECOND, p -> gnrs.playWithVersion(new GUID(101), -2));
        Timeline.addEvent(15 * Timeline.SECOND, p -> r2.registerDataConsumer(new GUID(101), false, dataConsumer));
        Timeline.addEvent(20 * Timeline.SECOND, p -> r3.registerDataConsumer(new GUID(101), false, dataConsumer));
        Timeline.run();

        //TODO add verify
    }

    @Test
    public void test10() {
        LOG.log(Level.INFO, "Duplicate requests");

        ReportingMFGNRS gnrs = new ReportingMFGNRS("GNRS", new UnlimitedQueue<>());
        MFRouter r1 = new MFRouter("R1", new UnlimitedQueue<>(), gnrs.getNa());
        MFRouter r2 = new MFRouter("R2", new UnlimitedQueue<>(), gnrs.getNa());
        MFRouter r3 = new MFRouter("R3", new UnlimitedQueue<>(), gnrs.getNa());

        MFRouter[] nodes = new MFRouter[]{gnrs, r1, r2, r3};

        Node.linkNodes(r1, gnrs, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(r2, gnrs, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(r3, r1, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());

        for (MFRouter node : nodes) {
            Timeline.addEvent(0, p -> ((MFRouter) p[0]).announceNA(), node);
            Timeline.run();
        }

        Timeline.addEvent(0, p -> r3.enqueueIncomingData(r3, new MFApplicationPacketData(new GUID(99), new GUID(101), new RandomData(1000 * Data.BYTE)), false));
        Timeline.addEvent(1 * Timeline.SECOND, p -> r1.enqueueIncomingData(r1, new MFApplicationPacketData(new GUID(100), new GUID(101), new RandomData(1000 * Data.BYTE)), false));

        BiConsumer<MFRouter, MFApplicationPacket> dataConsumer = (r, d) -> {
            System.out.printf("[%d] %s received G:%d->G:%d, len=%d%n",
                    Timeline.nowInUs(),
                    r.getName(),
                    d.getSrc().getRepresentation(),
                    d.getDst().getRepresentation(),
                    d.getSizeInBits() / Data.BYTE);
        };
        Timeline.addEvent(10 * Timeline.SECOND, p -> r2.registerDataConsumer(new GUID(101), false, dataConsumer));
        Timeline.run();

        //TODO add verify
    }

    @Test
    public void test11() {
        LOG.log(Level.INFO, "Duplicate requests 2");

        ReportingMFGNRS gnrs = new ReportingMFGNRS("GNRS", new UnlimitedQueue<>());
        MFRouter r1 = new MFRouter("R1", new UnlimitedQueue<>(), gnrs.getNa());
        MFRouter r2 = new MFRouter("R2", new UnlimitedQueue<>(), gnrs.getNa());
        MFRouter r3 = new MFRouter("R3", new UnlimitedQueue<>(), gnrs.getNa());

        MFRouter[] nodes = new MFRouter[]{gnrs, r1, r2, r3};

        Node.linkNodes(r1, gnrs, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(r2, gnrs, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(r3, r1, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());

        for (MFRouter node : nodes) {
            Timeline.addEvent(0, p -> ((MFRouter) p[0]).announceNA(), node);
            Timeline.run();
        }

        Timeline.addEvent(0, p -> r3.enqueueIncomingData(r3, new MFApplicationPacketData(new GUID(98), new GUID(101), new RandomData(1000 * Data.BYTE)), false));
        Timeline.addEvent(1 * Timeline.SECOND, p -> r3.enqueueIncomingData(r3, new MFApplicationPacketData(new GUID(99), new GUID(101), new RandomData(1000 * Data.BYTE)), false));
        Timeline.addEvent(1 * Timeline.SECOND, p -> r1.enqueueIncomingData(r1, new MFApplicationPacketData(new GUID(100), new GUID(101), new RandomData(1000 * Data.BYTE)), false));

        Timeline.addEvent(15 * Timeline.SECOND, p -> r3.enqueueIncomingData(r3, new MFApplicationPacketData(new GUID(98), new GUID(101), new RandomData(1000 * Data.BYTE)), false));
        Timeline.addEvent(15 * Timeline.SECOND + 110 * Timeline.MS, p -> r1.enqueueIncomingData(r1, new MFApplicationPacketData(new GUID(100), new GUID(101), new RandomData(1000 * Data.BYTE)), false));
        Timeline.addEvent(15 * Timeline.SECOND + 110 * Timeline.MS, p -> r3.enqueueIncomingData(r3, new MFApplicationPacketData(new GUID(98), new GUID(101), new RandomData(1000 * Data.BYTE)), false));
        Timeline.addEvent(15 * Timeline.SECOND + 210 * Timeline.MS, p -> r1.enqueueIncomingData(r1, new MFApplicationPacketData(new GUID(100), new GUID(101), new RandomData(1000 * Data.BYTE)), false));

        Timeline.addEvent(23 * Timeline.SECOND, p -> r3.enqueueIncomingData(r3, new MFApplicationPacketData(new GUID(98), new GUID(101), new RandomData(1000 * Data.BYTE)), false));
        Timeline.addEvent(23 * Timeline.SECOND + 110 * Timeline.MS, p -> r1.enqueueIncomingData(r1, new MFApplicationPacketData(new GUID(100), new GUID(101), new RandomData(1000 * Data.BYTE)), false));
        Timeline.addEvent(23 * Timeline.SECOND + 110 * Timeline.MS, p -> r3.enqueueIncomingData(r3, new MFApplicationPacketData(new GUID(98), new GUID(101), new RandomData(1000 * Data.BYTE)), false));
        Timeline.addEvent(23 * Timeline.SECOND + 210 * Timeline.MS, p -> r1.enqueueIncomingData(r1, new MFApplicationPacketData(new GUID(100), new GUID(101), new RandomData(1000 * Data.BYTE)), false));

        Timeline.addEvent(30 * Timeline.SECOND, p -> r3.enqueueIncomingData(r3, new MFApplicationPacketData(new GUID(98), new GUID(101), new RandomData(1000 * Data.BYTE)), false));
        Timeline.addEvent(30 * Timeline.SECOND + 210 * Timeline.MS, p -> r3.enqueueIncomingData(r3, new MFApplicationPacketData(new GUID(98), new GUID(101), new RandomData(1000 * Data.BYTE)), false));

        Timeline.addEvent(50 * Timeline.SECOND, p -> gnrs.playWithVersion(new GUID(101), -1));
        Timeline.addEvent(50 * Timeline.SECOND, p -> r3.enqueueIncomingData(r3, new MFApplicationPacketData(new GUID(98), new GUID(101), new RandomData(1000 * Data.BYTE)), false));
        Timeline.addEvent(55 * Timeline.SECOND, p -> gnrs.playWithVersion(new GUID(101), 1));

        BiConsumer<MFRouter, MFApplicationPacket> dataConsumer = (r, d) -> {
            System.out.printf("[%d] %s received G:%d->G:%d, len=%d%n",
                    Timeline.nowInUs(),
                    r.getName(),
                    d.getSrc().getRepresentation(),
                    d.getDst().getRepresentation(),
                    d.getSizeInBits() / Data.BYTE);
        };
        Timeline.addEvent(11 * Timeline.SECOND, p -> r2.registerDataConsumer(new GUID(101), false, dataConsumer));
        Timeline.run();

        //TODO add verify
    }

    @Test
    public void test12() {
        LOG.log(Level.INFO, "Request without data");

        ReportingMFGNRS gnrs = new ReportingMFGNRS("GNRS", new UnlimitedQueue<>());
        MFRouter r1 = new MFRouter("R1", new UnlimitedQueue<>(), gnrs.getNa());
        MFRouter r2 = new MFRouter("R2", new UnlimitedQueue<>(), gnrs.getNa());
        MFRouter r3 = new MFRouter("R3", new UnlimitedQueue<>(), gnrs.getNa());

        MFRouter[] nodes = new MFRouter[]{gnrs, r1, r2, r3};

        Node.linkNodes(r1, gnrs, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(r2, gnrs, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(r3, r1, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());

        for (MFRouter node : nodes) {
            Timeline.addEvent(0, p -> ((MFRouter) p[0]).announceNA(), node);
            Timeline.run();
        }
        Timeline.addEvent(0, p -> r3.sendData(r3.getGnrsNa(), new MFHopPacketGNRSRequest(new GUID(101), r3.getNa()), true, 0));
        Timeline.run();

        //TODO add verify
    }

    @Test
    public void test13() {
        LOG.log(Level.INFO, "Broadcast associate");

        ReportingMFGNRS gnrs = new ReportingMFGNRS("GNRS", new UnlimitedQueue<>());
        MFRouter r1 = new MFRouter("R1", new UnlimitedQueue<>(), gnrs.getNa());
        MFRouter r2 = new MFRouter("R2", new UnlimitedQueue<>(), gnrs.getNa());
        MFRouter r3 = new MFRouter("R3", new UnlimitedQueue<>(), gnrs.getNa());

        MFRouter[] nodes = new MFRouter[]{gnrs, r1, r2, r3};

        Node.linkNodes(r1, gnrs, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(r2, gnrs, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(r3, r1, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());

        for (MFRouter node : nodes) {
            Timeline.addEvent(0, p -> ((MFRouter) p[0]).announceNA(), node);
            Timeline.run();
        }

        for (MFRouter node : nodes) {
            node.forEachFib((na, tup) -> {
                System.out.printf("%s->%s %s %d%n", node.getName(), na.getNode().getName(), tup.getV1().getName(), tup.getV2());
            });
        }

        Timeline.addEvent(0, p -> r3.enqueueIncomingData(r3, new MFApplicationPacketData(new GUID(98), new GUID(101), new RandomData(1000 * Data.BYTE)), false));
        Timeline.addEvent(1 * Timeline.SECOND, p -> r3.enqueueIncomingData(r3, new MFApplicationPacketData(new GUID(99), new GUID(101), new RandomData(1000 * Data.BYTE)), false));
        Timeline.addEvent(1 * Timeline.SECOND, p -> r1.enqueueIncomingData(r1, new MFApplicationPacketData(new GUID(100), new GUID(101), new RandomData(1000 * Data.BYTE)), false));

        BiConsumer<MFRouter, MFApplicationPacket> dataConsumer = (r, d) -> {
            System.out.printf("[%d] %s received G:%d->G:%d, len=%d%n",
                    Timeline.nowInUs(),
                    r.getName(),
                    d.getSrc().getRepresentation(),
                    d.getDst().getRepresentation(),
                    d.getSizeInBits() / Data.BYTE);
        };
        Timeline.addEvent(11 * Timeline.SECOND, p -> r2.registerDataConsumer(new GUID(101), true, dataConsumer));

        Timeline.addEvent(12 * Timeline.SECOND, p -> {
            r1.forEachNrsCache((guid, tup) -> {
                System.out.printf("GUID:%d NAs:%s, Version:%d, Expire:%d%n",
                        guid.getRepresentation(),
                        Arrays.toString(Stream.of(tup.getV1()).map(na -> na.getNode().getName()).toArray()),
                        tup.getV2(),
                        tup.getV3());
            });
            r1.activeNrsCacheStream().forEach(e -> {
                System.out.printf("GUID:%d NAs:%s, Version:%d, Expire:%d%n",
                        e.getKey().getRepresentation(),
                        Arrays.toString(Stream.of(e.getValue().getV1()).map(na -> na.getNode().getName()).toArray()),
                        e.getValue().getV2(),
                        e.getValue().getV3());
            });
            r1.nrsCacheStream().forEach(e -> {
                System.out.printf("GUID:%d NAs:%s, Version:%d, Expire:%d%n",
                        e.getKey().getRepresentation(),
                        Arrays.toString(Stream.of(e.getValue().getV1()).map(na -> na.getNode().getName()).toArray()),
                        e.getValue().getV2(),
                        e.getValue().getV3());
            });
        });
        Timeline.addEvent(18 * Timeline.SECOND, p -> {
            r1.forEachNrsCache((guid, tup) -> {
                System.out.printf(">> GUID:%d NAs:%s, Version:%d, Expire:%d%n",
                        guid.getRepresentation(),
                        Arrays.toString(Stream.of(tup.getV1()).map(na -> na.getNode().getName()).toArray()),
                        tup.getV2(),
                        tup.getV3());
            });
            r1.activeNrsCacheStream().forEach(e -> {
                System.out.printf(">> GUID:%d NAs:%s, Version:%d, Expire:%d%n",
                        e.getKey().getRepresentation(),
                        Arrays.toString(Stream.of(e.getValue().getV1()).map(na -> na.getNode().getName()).toArray()),
                        e.getValue().getV2(),
                        e.getValue().getV3());
            });
            r1.nrsCacheStream().forEach(e -> {
                System.out.printf(">> GUID:%d NAs:%s, Version:%d, Expire:%d%n",
                        e.getKey().getRepresentation(),
                        Arrays.toString(Stream.of(e.getValue().getV1()).map(na -> na.getNode().getName()).toArray()),
                        e.getValue().getV2(),
                        e.getValue().getV3());
            });
        });

        Timeline.addEvent(18 * Timeline.SECOND, p -> {
            gnrs.forEachStorage((guid, tup) -> {
                System.out.printf("--GNRS-- GUID:%d NAs:%s, Version:%d%n",
                        guid.getRepresentation(),
                        Arrays.toString(tup.getV1().map(na -> na.getNode().getName()).toArray()),
                        tup.getV2());
            });
            gnrs.getStorageStream().forEach(e -> {
                System.out.printf("--GNRS-- GUID:%d NAs:%s, Version:%d%n",
                        e.getKey().getRepresentation(),
                        Arrays.toString(e.getValue().getV1().map(na -> na.getNode().getName()).toArray()),
                        e.getValue().getV2());
            });
        });
        Timeline.run();

    }

    @Test
    public void test14() {
        LOG.log(Level.INFO, "Test nrs cache");

        ReportingMFGNRS gnrs = new ReportingMFGNRS("GNRS", new UnlimitedQueue<>());
        MFRouter r1 = new MFRouter("R1", new UnlimitedQueue<>(), gnrs.getNa());
        MFRouter r2 = new MFRouter("R2", new UnlimitedQueue<>(), gnrs.getNa());
        MFRouter r3 = new MFRouter("R3", new UnlimitedQueue<>(), gnrs.getNa());
        MFRouter r4 = new MFRouter("R4", new UnlimitedQueue<>(), gnrs.getNa());

        MFRouter[] nodes = new MFRouter[]{gnrs, r1, r2, r3, r4};

        Node.linkNodes(r1, gnrs, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(r2, gnrs, 100 * Node.BW_IN_MBPS, 200 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(r4, gnrs, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(r3, r1, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());

        for (MFRouter node : nodes) {
            Timeline.addEvent(0, p -> ((MFRouter) p[0]).announceNA(), node);
            Timeline.run();
        }

        BiConsumer<MFRouter, MFApplicationPacket> dataConsumer = (r, d) -> {
            System.out.printf("[%d] %s received G:%d->G:%d, len=%d%n",
                    Timeline.nowInUs(),
                    r.getName(),
                    d.getSrc().getRepresentation(),
                    d.getDst().getRepresentation(),
                    d.getSizeInBits() / Data.BYTE);
        };
        Timeline.addEvent(8 * Timeline.SECOND, p -> r4.enqueueIncomingData(r4, new MFApplicationPacketData(new GUID(102), new GUID(101), new RandomData(1002 * Data.BYTE)), false));
        Timeline.addEvent(11 * Timeline.SECOND, p -> r4.registerDataConsumer(new GUID(101), false, dataConsumer));

        Timeline.addEvent(12 * Timeline.SECOND, p -> r3.enqueueIncomingData(r3, new MFApplicationPacketData(new GUID(99), new GUID(101), new RandomData(1003 * Data.BYTE)), false));
        Timeline.addEvent(12 * Timeline.SECOND, p -> r1.enqueueIncomingData(r1, new MFApplicationPacketData(new GUID(100), new GUID(101), new RandomData(1004 * Data.BYTE)), false));
        Timeline.addEvent(12 * Timeline.SECOND, p -> r4.enqueueIncomingData(r4, new MFApplicationPacketData(new GUID(102), new GUID(101), new RandomData(1005 * Data.BYTE)), false));
        Timeline.addEvent(12 * Timeline.SECOND, p -> r4.forEachNrsCache(MFPubSubRouterTest::printGNRSCacheEntry));

        Timeline.addEvent(13 * Timeline.SECOND, p -> r4.deregisterDataConsumer(new GUID(101), false, dataConsumer));
        Timeline.addEvent(13 * Timeline.SECOND, p -> r4.enqueueIncomingData(r4, new MFApplicationPacketData(new GUID(102), new GUID(101), new RandomData(1006 * Data.BYTE)), false));

        Timeline.addEvent(15 * Timeline.SECOND, p -> r2.registerDataConsumer(new GUID(101), false, dataConsumer));

        Timeline.run();
    }

@Test
    public void test15() {
        LOG.log(Level.INFO, "Test nrs cache");

        ReportingMFGNRS gnrs = new ReportingMFGNRS("GNRS", new UnlimitedQueue<>());
        MFRouter r1 = new MFRouter("R1", new UnlimitedQueue<>(), gnrs.getNa());
        MFRouter r2 = new MFRouter("R2", new UnlimitedQueue<>(), gnrs.getNa());
        MFRouter r3 = new MFRouter("R3", new UnlimitedQueue<>(), gnrs.getNa());
        MFRouter r4 = new MFRouter("R4", new UnlimitedQueue<>(), gnrs.getNa());

        MFRouter[] nodes = new MFRouter[]{gnrs, r1, r2, r3, r4};

        Node.linkNodes(r1, gnrs, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(r2, gnrs, 100 * Node.BW_IN_MBPS, 200 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(r4, gnrs, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(r3, r1, 100 * Node.BW_IN_MBPS, 100 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());

        for (MFRouter node : nodes) {
            Timeline.addEvent(0, p -> ((MFRouter) p[0]).announceNA(), node);
            Timeline.run();
        }

        BiConsumer<MFRouter, MFApplicationPacket> dataConsumer = (r, d) -> {
            System.out.printf("[%d] %s received G:%d->G:%d, len=%d%n",
                    Timeline.nowInUs(),
                    r.getName(),
                    d.getSrc().getRepresentation(),
                    d.getDst().getRepresentation(),
                    d.getSizeInBits() / Data.BYTE);
        };
        Timeline.addEvent(8 * Timeline.SECOND, p -> r2.enqueueIncomingData(r2, new MFApplicationPacketData(new GUID(98), new GUID(101), new RandomData(1001 * Data.BYTE)), false));
        Timeline.addEvent(8 * Timeline.SECOND, p -> r4.enqueueIncomingData(r4, new MFApplicationPacketData(new GUID(102), new GUID(101), new RandomData(1002 * Data.BYTE)), false));
        Timeline.addEvent(11 * Timeline.SECOND, p -> r4.registerDataConsumer(new GUID(101), false, dataConsumer));
        Timeline.addEvent(11 * Timeline.SECOND, p -> r2.registerDataConsumer(new GUID(101), false, dataConsumer));

        Timeline.addEvent(12 * Timeline.SECOND, p -> r3.enqueueIncomingData(r3, new MFApplicationPacketData(new GUID(99), new GUID(101), new RandomData(1003 * Data.BYTE)), false));
        Timeline.addEvent(12 * Timeline.SECOND, p -> r1.enqueueIncomingData(r1, new MFApplicationPacketData(new GUID(100), new GUID(101), new RandomData(1004 * Data.BYTE)), false));
        Timeline.addEvent(12 * Timeline.SECOND, p -> r4.enqueueIncomingData(r4, new MFApplicationPacketData(new GUID(102), new GUID(101), new RandomData(1005 * Data.BYTE)), false));
        Timeline.addEvent(12 * Timeline.SECOND, p -> r4.forEachNrsCache(MFPubSubRouterTest::printGNRSCacheEntry));

        Timeline.addEvent(13 * Timeline.SECOND, p -> r4.deregisterDataConsumer(new GUID(101), false, dataConsumer));
        Timeline.addEvent(13 * Timeline.SECOND, p -> r4.enqueueIncomingData(r4, new MFApplicationPacketData(new GUID(102), new GUID(101), new RandomData(1006 * Data.BYTE)), false));
        Timeline.addEvent(13 * Timeline.SECOND, p -> r3.enqueueIncomingData(r3, new MFApplicationPacketData(new GUID(99), new GUID(101), new RandomData(1007 * Data.BYTE)), false));
        Timeline.addEvent(13 * Timeline.SECOND, p -> r1.enqueueIncomingData(r1, new MFApplicationPacketData(new GUID(100), new GUID(101), new RandomData(1008 * Data.BYTE)), false));

        Timeline.run();

    }
}

package edu.rutgers.winlab.networksimulator.network.mf;

import static edu.rutgers.winlab.networksimulator.common.Helper.assertStreamEquals;
import edu.rutgers.winlab.networksimulator.common.Timeline;
import edu.rutgers.winlab.networksimulator.common.TriConsumer;
import edu.rutgers.winlab.networksimulator.common.Tuple2;
import edu.rutgers.winlab.networksimulator.common.UnlimitedQueue;
import edu.rutgers.winlab.networksimulator.network.Node;
import edu.rutgers.winlab.networksimulator.network.mf.packets.GUID;
import edu.rutgers.winlab.networksimulator.network.mf.packets.MFPacketData;
import edu.rutgers.winlab.networksimulator.network.mf.packets.NA;
import java.util.HashMap;
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

        BiConsumer<Node, MFPacketData> consumer1 = (n, d) -> {
            System.out.printf("%s Got: %s%n", n.getName(), d);
        };
        BiConsumer<Node, MFPacketData> consumer2 = (n, d) -> {
            System.out.printf("%s Got 2: %s%n", n.getName(), d);
        };

        int gargetGUID = 100;
        Tuple2<Stream<NA>, Integer> ret;
        NA na1 = routers.get("R1").getNa();
        NA na3 = routers.get("R3").getNa();

        ret = gnrs.getGuidValue(new GUID(gargetGUID - 1));
        assertStreamEquals(Stream.empty(), ret.getV1());
        assertEquals(1, (int) ret.getV2());

        routers.get("R1").registerDataConsumer(new GUID(gargetGUID), consumer1);
        Timeline.run();
        ret = gnrs.getGuidValue(new GUID(gargetGUID));
        assertStreamEquals(Stream.of(na1), ret.getV1());
        assertEquals(2, (int) ret.getV2());

        routers.get("R3").registerDataConsumer(new GUID(gargetGUID), consumer1);
        Timeline.run();
        ret = gnrs.getGuidValue(new GUID(gargetGUID));
        assertStreamEquals(Stream.of(na1, na3), ret.getV1().sorted());
        assertEquals(3, (int) ret.getV2());

        routers.get("R3").registerDataConsumer(new GUID(gargetGUID), consumer2);
        Timeline.run();
        ret = gnrs.getGuidValue(new GUID(gargetGUID));
        assertStreamEquals(Stream.of(na1, na3), ret.getV1().sorted());
        assertEquals(3, (int) ret.getV2());

        routers.get("R3").deregisterDataConsumer(new GUID(gargetGUID), consumer1);
        Timeline.run();
        ret = gnrs.getGuidValue(new GUID(gargetGUID));
        assertStreamEquals(Stream.of(na1, na3), ret.getV1().sorted());
        assertEquals(3, (int) ret.getV2());

        routers.get("R3").deregisterDataConsumer(new GUID(gargetGUID), consumer2);
        Timeline.run();
        ret = gnrs.getGuidValue(new GUID(gargetGUID));
        assertStreamEquals(Stream.of(na1), ret.getV1().sorted());
        assertEquals(4, (int) ret.getV2());

        routers.get("R1").deregisterDataConsumer(new GUID(gargetGUID), consumer2);
        Timeline.run();
        ret = gnrs.getGuidValue(new GUID(gargetGUID));
        assertStreamEquals(Stream.of(na1), ret.getV1().sorted());
        assertEquals(4, (int) ret.getV2());

        routers.get("R1").deregisterDataConsumer(new GUID(gargetGUID), consumer1);
        Timeline.run();
        ret = gnrs.getGuidValue(new GUID(gargetGUID));
        assertStreamEquals(Stream.empty(), ret.getV1().sorted());
        assertEquals(5, (int) ret.getV2());

        routers.get("R1").deregisterDataConsumer(new GUID(gargetGUID - 1), consumer1);
        Timeline.run();
        ret = gnrs.getGuidValue(new GUID(gargetGUID));
        assertStreamEquals(Stream.empty(), ret.getV1().sorted());
        assertEquals(5, (int) ret.getV2());
    }
}

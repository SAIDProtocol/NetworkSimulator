/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.networksimulator.network;

import edu.rutgers.winlab.networksimulator.common.Data;
import static edu.rutgers.winlab.networksimulator.common.Helper.assertStreamEquals;
import edu.rutgers.winlab.networksimulator.common.ItemLimitedQueue;
import edu.rutgers.winlab.networksimulator.common.PrioritizedQueue;
import edu.rutgers.winlab.networksimulator.common.RandomData;
import edu.rutgers.winlab.networksimulator.common.Timeline;
import edu.rutgers.winlab.networksimulator.common.Tuple1;
import edu.rutgers.winlab.networksimulator.common.Tuple2;
import edu.rutgers.winlab.networksimulator.common.Tuple4;
import edu.rutgers.winlab.networksimulator.common.UnlimitedQueue;
import edu.rutgers.winlab.networksimulator.network.Node.UnicastLink;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.junit.AfterClass;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jiachen
 */
public class NodeTest {

    private static final Logger LOG = Logger.getLogger(NodeTest.class.getName());

    public NodeTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    private static class MyData extends RandomData {

        private static int SERIAL = 1;

        private final int serial = SERIAL++;

        public MyData(int sizeInBits) {
            super(sizeInBits);
        }

        public int getSerial() {
            return serial;
        }

        public Data copy() {
            return new MyData(getSizeInBits());
        }

        @Override
        public String toString() {
            return "MyData{" + "serial=" + serial + '}';
        }

    }

    private static class TestNode extends Node {

        private final long processDuration;
        private final ArrayList<Tuple4<Long, Node, Node, Integer>> result;
        private final HashMap<Node, Node> fib = new HashMap<>();

        public TestNode(String name, PrioritizedQueue<Tuple2<Node, Data>> incomingQueue, long processDuration, ArrayList<Tuple4<Long, Node, Node, Integer>> result) {
            super(name, incomingQueue);
            this.processDuration = processDuration;
            this.result = result;
        }

        public void addFib(Node from, Node to) {
            fib.put(from, to);
        }

        @Override
        public void sendUnicastData(Node destination, Data d, boolean prioritized) {
            super.sendUnicastData(destination, d, prioritized);
        }

        @Override
        protected long handleData(Tuple2<Node, Data> t) {
            MyData md = (MyData) t.getV2();
            result.add(new Tuple4<>(Timeline.nowInUs(), this, t.getV1(), md.getSerial()));
//            LOG.log(Level.INFO, "[{2}] {3} Received serial {0} from {1}", new Object[]{md.getSerial(), t.getV1().getName(), Timeline.nowInUs(), getName()});
            Node target = fib.get(t.getV1());
            if (target != null) {
                Timeline.addEvent(Timeline.nowInUs() + processDuration,
                        e -> {
                            sendUnicastData((Node) e[0], md.copy(), false);
                            sendUnicastData((Node) e[0], md.copy(), false);
                        },
                        target, md);
            }
            return processDuration;
        }
    }

    @Test
    public void test1() {
        MyData.SERIAL = 1;
        ArrayList<Tuple4<Long, Node, Node, Integer>> result = new ArrayList<>();
        TestNode n1 = new TestNode("N1", new UnlimitedQueue<>(), 0, result);
        TestNode n2 = new TestNode("N2", new UnlimitedQueue<>(), 2000, result);
        TestNode n3 = new TestNode("N3", new UnlimitedQueue<>(), 1050, result);
        Node.linkNodes(n1, n2, 1 * Node.BW_IN_MBPS, 1 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(n2, n3, 1 * Node.BW_IN_MBPS, 1 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        n2.addFib(n1, n3);
        n2.addFib(n3, n1);
        n3.addFib(n2, n2);

        Stream<Tuple4<Long, Node, Node, Integer>> target = Stream.of(
                new Tuple4<>(2000L, n2, n1, 1),
                new Tuple4<>(6000L, n3, n2, 2),
                new Tuple4<>(7050L, n3, n2, 3),
                new Tuple4<>(9050L, n2, n3, 4),
                new Tuple4<>(11050L, n2, n3, 5),
                new Tuple4<>(13050L, n1, n2, 8),
                new Tuple4<>(13050L, n2, n3, 6),
                new Tuple4<>(14050L, n1, n2, 9),
                new Tuple4<>(15050L, n2, n3, 7),
                new Tuple4<>(15050L, n1, n2, 10),
                new Tuple4<>(16050L, n1, n2, 11),
                new Tuple4<>(17050L, n1, n2, 12),
                new Tuple4<>(18050L, n1, n2, 13),
                new Tuple4<>(19050L, n1, n2, 14),
                new Tuple4<>(20050L, n1, n2, 15)
        );
        ArrayList<Node> tmp = new ArrayList<>();
        n2.forEachUnicastLink(l -> tmp.add(l.getDestination()));
        tmp.sort((na, nb) -> na.getName().compareTo(nb.getName()));
        assertArrayEquals(new Object[]{n1, n3}, tmp.toArray());

        try {
            Node.linkNodes(n1, n2, 1 * Node.BW_IN_MBPS, 1, new UnlimitedQueue<>(), new UnlimitedQueue<>());
            fail("Should not reach here!");
        } catch (IllegalArgumentException e) {
        }

        Timeline.addEvent(0, ps -> {
            n1.sendUnicastData(n2, new MyData(1000), false);
            try {
                n1.sendUnicastData(n3, new RandomData(1000), false);
                fail("Should not reach here!");
            } catch (IllegalArgumentException e) {
            }
        });
        Timeline.addEvent(999, ps -> {
            assertTrue(n1.unicastLinkStream().allMatch(l -> l.isBusy()));
            assertFalse(n2.unicastLinkStream().anyMatch(l -> l.isBusy()));
        });
        Timeline.addEvent(4001, ps -> {
            n1.forEachUnicastLink(l -> {
                assertEquals(false, l.isBusy());
            });
            n2.forEachUnicastLink(l -> {
                assertEquals(l.getDestination() == n3, l.isBusy());
            });
        });

        assertEquals(20050L, Timeline.run());
        n2.forEachUnicastLink(l -> {
            assertEquals(n2, l.getSource());
            assertEquals(0, l.getBitsDiscarded());
            assertEquals(false, l.isBusy());
            assertEquals(true, l.isConnected());
            assertEquals(1 * Node.BW_IN_MBPS, l.getBwBitsPerMS());
            assertEquals(1 * Timeline.MS, l.getDelayInUS());
            if (l.getDestination() == n1) {
                assertEquals(8000L, l.getBitsSent());
            } else if (l.getDestination() == n3) {
                assertEquals(2000L, l.getBitsSent());
            } else {
                fail("Should not reach here!");
            }
        });
        assertStreamEquals(target, result.stream());
    }

    @Test
    public void test2() {
        MyData.SERIAL = 1;
        ArrayList<Tuple4<Long, Node, Node, Integer>> result = new ArrayList<>();
        TestNode n1 = new TestNode("N1", new UnlimitedQueue<>(), 0, result);
        TestNode n2 = new TestNode("N2", new UnlimitedQueue<>(), 2000, result);
        TestNode n3 = new TestNode("N3", new UnlimitedQueue<>(), 2000, result);
        Node.linkNodes(n1, n2, 1 * Node.BW_IN_MBPS, 1 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Tuple2<UnicastLink, UnicastLink> ret = Node.disconnectNodes(n1, n3);
        assertEquals(new Tuple2<>(null, null), ret);

        // pkt 2 is killed on the way, pkt 3 is killed when sending, pkt 4 is killed in the queue.
        Timeline.addEvent(0, ps -> {
            n1.sendUnicastData(n2, new MyData(1000), false); // pkt 1
            n1.sendUnicastData(n2, new MyData(1000), false); // pkt 2
            n1.sendUnicastData(n2, new MyData(1000), false); // pkt 3
            n1.sendUnicastData(n2, new MyData(1000), false); // pkt 4
            n2.sendUnicastData(n1, new MyData(1000), false); // pkt 5
            n2.sendUnicastData(n1, new MyData(1000), false); // pkt 6
        });
        Consumer<Node.AbstractLink> linkIdleHandler = l -> {
            UnicastLink ul = (UnicastLink) l;
            assertEquals(4000L, Timeline.nowInUs());
            if (l.getSource() == n1) {
                assertEquals(ul.getDestination(), n2);
                assertEquals(1000, l.getBitsSent());
                assertEquals(3000, l.getBitsDiscarded());
            } else {
                fail("Should not reach here!");
            }
        };
        Tuple1<UnicastLink> tmpStorage = new Tuple1<>(null);
        Timeline.addEvent(2500, ps -> {
            Tuple2<UnicastLink, UnicastLink> t = Node.disconnectNodes(n1, n2);
            t.getV1().addIdleHandler(linkIdleHandler);
            t.getV2().addIdleHandler(linkIdleHandler);
            tmpStorage.setV1(t.getV2());
        });
        Timeline.addEvent(2999, ps -> {
            tmpStorage.getV1().removeIdleHandler(linkIdleHandler);
        });

        assertEquals(4000L, Timeline.run());
        assertStreamEquals(Stream.of(
                new Tuple4<>(2000L, n2, n1, 1),
                new Tuple4<>(2000L, n1, n2, 5)), result.stream());
    }

    @Test
    public void test3() {
        MyData.SERIAL = 1;
        ArrayList<Tuple4<Long, Node, Node, Integer>> result = new ArrayList<>();
        TestNode n1 = new TestNode("N1", new UnlimitedQueue<>(), 0, result);
        TestNode n2 = new TestNode("N2", new ItemLimitedQueue<>(3), 2000, result);
        Node.linkNodes(n1, n2, 1 * Node.BW_IN_MBPS, 1 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Timeline.addEvent(0, ps -> {
            for (int i = 0; i < 10; i++) {
                n1.sendUnicastData(n2, new MyData(500), false);
            }
        });
        assertEquals(13500L, Timeline.run());

        assertStreamEquals(Stream.of(
                new Tuple4<>(1500L, n2, n1, 1),
                new Tuple4<>(3500L, n2, n1, 2),
                new Tuple4<>(5500L, n2, n1, 3),
                new Tuple4<>(7500L, n2, n1, 4),
                new Tuple4<>(9500L, n2, n1, 5),
                new Tuple4<>(11500L, n2, n1, 9)
        ), result.stream());
        assertStreamEquals(Stream.of(new Tuple2<>(n1, 2000L)), n2.bitsDiscardedStream());
        n2.forEachBitsDiscarded((n, b) -> {
            assertEquals(n1, n);
            assertEquals((Long) 2000L, b);
        });
    }

    private static class Satellite extends Node {

        private final long processingTime;
        private final String broadcastChannelName;
        private final ArrayList<Tuple4<Long, Node, Node, Integer>> result;

        public Satellite(String name, PrioritizedQueue<Tuple2<Node, Data>> incomingQueue,
                long processingTime, ArrayList<Tuple4<Long, Node, Node, Integer>> result,
                String broadcastChannelName, int broadcastChannelBwInKbps, long broadcastChannelDelayInUs,
                PrioritizedQueue<Data> broadcastChannelQueue) {
            super(name, incomingQueue);
            this.processingTime = processingTime;
            this.broadcastChannelName = broadcastChannelName;
            this.result = result;
            createBroadcastChannel(broadcastChannelName, broadcastChannelBwInKbps, broadcastChannelDelayInUs, broadcastChannelQueue);
        }

        @Override
        protected long handleData(Tuple2<Node, Data> t) {
            MyData md = (MyData) t.getV2();
            result.add(new Tuple4<>(Timeline.nowInUs(), this, t.getV1(), md.getSerial()));
            sendBroadcastData(broadcastChannelName, md.copy(), false);
            return processingTime;
        }

        public boolean addReceiver(Node n) {
            return getBroadcastChannel(broadcastChannelName).addNode(n);
        }

        public boolean removeReceiver(Node n, boolean needDiscardDataInFlight) {
            return getBroadcastChannel(broadcastChannelName).removeNode(n, needDiscardDataInFlight);
        }
    }

    @Test
    public void test4() {
        String broadcastChannelName = "Test";
        MyData.SERIAL = 1;
        int satelliteLinkBw = 10 * Node.BW_IN_MBPS;
        long satelliteLinkLatency = 150 * Timeline.MS;
        int packetSize = 500 * Data.BYTE;

        ArrayList<Tuple4<Long, Node, Node, Integer>> result = new ArrayList<>();

        Satellite satellite = new Satellite("SATELLITE", new UnlimitedQueue<>(), 0, result,
                broadcastChannelName, satelliteLinkBw, satelliteLinkLatency, new UnlimitedQueue<>());
        TestNode sender = new TestNode("SENDER", new UnlimitedQueue<>(), 0, result);
        TestNode n3 = new TestNode("N3", new ItemLimitedQueue<>(3), 0, result);
        TestNode n4 = new TestNode("N4", new ItemLimitedQueue<>(3), 1000, result);
        TestNode n5 = new TestNode("N5", new ItemLimitedQueue<>(3), 0, result);
        TestNode n6 = new TestNode("N6", new ItemLimitedQueue<>(3), 0, result);

        satellite.addReceiver(n3);
        satellite.addReceiver(n4);
        satellite.addReceiver(n5);
        satellite.addReceiver(n6);
        Node.linkNodes(sender, satellite, satelliteLinkBw, satelliteLinkLatency, new UnlimitedQueue<>(), new UnlimitedQueue<>());

        try {
            satellite.sendBroadcastData("$WRONG!$", new MyData(10), true);
            fail();
        } catch (Exception e) {

        }
        assertFalse(satellite.createBroadcastChannel(broadcastChannelName, satelliteLinkBw, satelliteLinkLatency, new UnlimitedQueue<>()));

        Timeline.addEvent(0, ps -> {
            for (int i = 0; i < 16; i++) {
                sender.sendUnicastData(satellite, new MyData(packetSize), false);
            }
        });

        System.out.printf("B=%,d, BW=%,d, R=%,d%n", packetSize, satelliteLinkBw, Node.getTransmitTimeInUs(packetSize, satelliteLinkBw));
        Timeline.addEvent(satelliteLinkLatency * 2 + Node.getTransmitTimeInUs(packetSize, satelliteLinkBw) * 4 - 1, ps -> {
            System.out.printf("%,d%n", Timeline.nowInUs());
            satellite.getBroadcastChannel(broadcastChannelName).removeNode(n5, false);
            satellite.getBroadcastChannel(broadcastChannelName).removeNode(n6, true);
            satellite.getBroadcastChannel(broadcastChannelName).removeNode(n5, false);
            satellite.getBroadcastChannel(broadcastChannelName).removeNode(n5, true);
            satellite.getBroadcastChannel(broadcastChannelName).addNode(n6);
            satellite.getBroadcastChannel(broadcastChannelName).removeNode(n6, true);
        });
        Timeline.addEvent(satelliteLinkLatency * 2 + Node.getTransmitTimeInUs(packetSize, satelliteLinkBw) * 10 - 1, ps -> {
            System.out.printf("%,d%n", Timeline.nowInUs());
            satellite.getBroadcastChannel(broadcastChannelName).abort();
        });
        Timeline.run();

        result.forEach(t -> {
            System.out.printf("[%,d] %s->%s %d%n", t.getV1(), t.getV2().getName(), t.getV3().getName(), t.getV4());
        });

        satellite.forEachBroadcastLink((n, l) -> System.out.printf("%s D:%,d, S:%,d%n", n, l.getBitsDiscarded(), l.getBitsSent()));
        satellite.broadcastLinkStream().forEach(e -> System.out.printf("%s D:%,d, S:%,d%n", e.getKey(), e.getValue().getBitsDiscarded(), e.getValue().getBitsSent()));
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.networksimulator.network;

import edu.rutgers.winlab.networksimulator.common.Data;
import edu.rutgers.winlab.networksimulator.common.PrioritizedQueue;
import edu.rutgers.winlab.networksimulator.common.RandomData;
import edu.rutgers.winlab.networksimulator.common.Timeline;
import edu.rutgers.winlab.networksimulator.common.Tuple2;
import edu.rutgers.winlab.networksimulator.common.UnlimitedQueue;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.AfterClass;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
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

    private static class TestNode extends Node {

        private final long processDuration;

        public TestNode(String name, PrioritizedQueue<Tuple2<Node, Data>> incomingQueue, long processDuration) {
            super(name, incomingQueue);
            this.processDuration = processDuration;
        }

        @Override
        public void sendData(Node destination, Data d, boolean prioritized) {
            super.sendData(destination, d, prioritized);
        }

        @Override
        protected long handleData(Tuple2<Node, Data> t) {

            LOG.log(Level.INFO, "[{2}] {3} Received {0} bits from {1}", new Object[]{t.getV2().getSizeInBits(), t.getV1().getName(), Timeline.nowInUs(), getName()});
            return processDuration;
        }

    }

    @Test
    public void test1() {
        TestNode n1 = new TestNode("N1", new UnlimitedQueue<>(), 0);
        TestNode n2 = new TestNode("N2", new UnlimitedQueue<>(), 2000);
        TestNode n3 = new TestNode("N3", new UnlimitedQueue<>(), 0);
        Node.linkNodes(n1, n2, 1 * Node.BW_IN_MBPS, 1 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());
        Node.linkNodes(n2, n3, 1 * Node.BW_IN_MBPS, 1 * Timeline.MS, new UnlimitedQueue<>(), new UnlimitedQueue<>());

        ArrayList<Node> tmp = new ArrayList<>();
        n2.forEachLink(l -> tmp.add(l.getDestination()));
        tmp.sort((na, nb) -> na.getName().compareTo(nb.getName()));
        assertArrayEquals(new Object[]{n1, n3}, tmp.toArray());

        try {
            Node.linkNodes(n1, n2, 1 * Node.BW_IN_MBPS, 1, new UnlimitedQueue<>(), new UnlimitedQueue<>());
            fail("Should not reach here!");
        } catch (IllegalArgumentException e) {
        }

        RandomData rd = new RandomData(1000);
        Timeline.addEvent(0, ps -> {
            n1.sendData(n2, rd.copy(), false);
            n1.sendData(n2, rd.copy(), false);
            n2.sendData(n1, rd.copy(), false);
            n2.sendData(n1, rd.copy(), false);
            try {
                n1.sendData(n3, rd.copy(), false);
                fail("Should not reach here!");
            } catch (IllegalArgumentException e) {
            }
        });
        Timeline.addEvent(2000, ps -> {
            n1.forEachLink(l -> {
                assertEquals(true, l.isBusy());
            });
            n2.forEachLink(l -> {
                assertEquals(l.getDestination() == n1, l.isBusy());
            });
        });
        Timeline.addEvent(2001, ps -> {
            n1.forEachLink(l -> {
                assertEquals(false, l.isBusy());
            });
            n2.forEachLink(l -> {
                assertEquals(false, l.isBusy());
            });
        });

        Timeline.run();
        n1.forEachLink(l -> {
            assertEquals(n2, l.getDestination());
            assertEquals(n1, l.getSource());
            assertEquals(0, l.getBitsDiscarded());
            assertEquals(2000, l.getBitsSent());
            assertEquals(false, l.isBusy());
            assertEquals(true, l.isConnected());
            assertEquals(1 * Node.BW_IN_MBPS, l.getBwBitsPerMS());
            assertEquals(1 * Timeline.MS, l.getDelayInUS());
        });
    }

}

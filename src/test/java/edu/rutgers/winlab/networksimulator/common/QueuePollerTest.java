package edu.rutgers.winlab.networksimulator.common;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.junit.AfterClass;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Jiachen Chen
 */
public class QueuePollerTest {

    private static final Logger LOG = Logger.getLogger(QueuePollerTest.class.getName());

    public QueuePollerTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    private class TestData implements Data {

        private final int sizeInBits;
        private final long handleTime;

        public TestData(int sizeInBits, long handleTime) {
            this.sizeInBits = sizeInBits;
            this.handleTime = handleTime;
        }

        public long getHandleTime() {
            return handleTime;
        }

        @Override
        public int getSizeInBits() {
            return sizeInBits;
        }

        @Override
        public Data copy() {
            return this;
        }

    }

    @Test
    public void test1() {
        Object[] target = new Object[]{0L, 1000L, 3000L, 7000L, 10000L, 15000L, 22000L, 28000L};
        ArrayList<Long> result = new ArrayList<>();

        PrioritizedQueue queue = new UnlimitedQueue();
        QueuePoller p = new QueuePoller(d -> {
            result.add(Timeline.nowInUs());
            return ((TestData) d).handleTime;
        }, queue);

        Consumer<Object[]> addEventHandler = objs
                -> p.enQueue(new TestData((Integer) objs[0], (Long) objs[1]), (Boolean) objs[2]);

        Timeline.addEvent(0, addEventHandler, 1000, 1000L, false);
        Timeline.addEvent(0, addEventHandler, 1000, 2000L, true);
        Timeline.addEvent(0, addEventHandler, 1000, 3000L, false);
        Timeline.addEvent(0, addEventHandler, 1000, 4000L, true);
        Timeline.addEvent(0, addEventHandler, 1000, 5000L, false);
        Timeline.addEvent(15000, addEventHandler, 1000, 6000L, false);
        Timeline.addEvent(15000, addEventHandler, 1000, 7000L, true);
        Timeline.addEvent(15000, addEventHandler, 1000, 0L, false);

        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());
        assertEquals(28000L, Timeline.run());
        assertArrayEquals(target, result.toArray());
        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());
    }

    @Test
    public void test2() {
        Object[] target = new Object[]{0L, 1000L, 3000L, 15000L, 21000L, 28000L};
        ArrayList<Long> result = new ArrayList<>();

        PrioritizedQueue queue = new UnlimitedQueue();
        QueuePoller p = new QueuePoller(d -> {
            result.add(Timeline.nowInUs());
            return ((TestData) d).handleTime;
        }, queue);

        Consumer<Object[]> addEventHandler = objs
                -> p.enQueue(new TestData((Integer) objs[0], (Long) objs[1]), (Boolean) objs[2]);

        Timeline.addEvent(0, addEventHandler, 1000, 1000L, false);
        Timeline.addEvent(0, addEventHandler, 2000, 2000L, true);
        Timeline.addEvent(0, addEventHandler, 3000, 3000L, false);
        Timeline.addEvent(0, addEventHandler, 4000, 4000L, true);
        Timeline.addEvent(0, addEventHandler, 5000, 5000L, false);
        Timeline.addEvent(15000, addEventHandler, 1000, 6000L, false);
        Timeline.addEvent(15000, addEventHandler, 1000, 7000L, true);
        Timeline.addEvent(15000, addEventHandler, 1000, 0L, false);
        Timeline.addEvent(4000, objs -> {
            // at time 4000, 1000 and 2000 are done, 4000 running. remaining: 3000 and 5000
            Helper.assertStreamEquals(p.stream().map(d -> ((TestData) d).handleTime), Stream.of(3000L, 5000L));
            assertEquals(8000, p.getSizeInBits());
            assertEquals(8000, p.clear());
            Helper.assertStreamEquals(p.stream(), Stream.of());
            assertEquals(0, p.getSizeInBits());

        });

        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());
        assertEquals(28000L, Timeline.run());
        assertArrayEquals(target, result.toArray());
        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());

    }

}

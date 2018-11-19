package edu.rutgers.winlab.networksimulator.common;

import static edu.rutgers.winlab.networksimulator.common.Helper.assertStreamEquals;
import java.util.ArrayList;
import java.util.function.Consumer;
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
 * @author Jiachen Chen
 */
public class QueuePollerTest {

//    private static final Logger LOG = Logger.getLogger(QueuePollerTest.class.getName());
    public QueuePollerTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Test
    public void test1() {
        Object[] target = new Object[]{0L, 1000L, 3000L, 7000L, 10000L, 15000L, 22000L, 28000L};
        Object[] idleTarget = new Object[]{28000L};
        ArrayList<Long> result = new ArrayList<>();
        ArrayList<Long> idleResult = new ArrayList<>();

        PrioritizedQueue<Long> queue = new UnlimitedQueue<>();
        QueuePoller<Long> p = new QueuePoller<>(l -> {
            result.add(Timeline.nowInUs());
            return l;
        }, queue, v -> idleResult.add(Timeline.nowInUs()));

        Consumer<Object[]> addEventHandler = objs -> p.enQueue((Long) objs[0], (Boolean) objs[1]);

        Timeline.addEvent(0, addEventHandler, 1000L, false);
        Timeline.addEvent(0, addEventHandler, 2000L, true);
        Timeline.addEvent(0, addEventHandler, 3000L, false);
        Timeline.addEvent(0, addEventHandler, 4000L, true);
        Timeline.addEvent(0, addEventHandler, 5000L, false);
        Timeline.addEvent(15000, addEventHandler, 6000L, false);
        Timeline.addEvent(15000, addEventHandler, 7000L, true);
        Timeline.addEvent(15000, addEventHandler, 0L, false);

        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());
        assertEquals(28000L, Timeline.run());
        assertArrayEquals(idleTarget, idleResult.toArray());
        assertArrayEquals(target, result.toArray());
        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());
    }

    @Test
    public void test2() {
        Object[] target = new Object[]{0L, 1000L, 3000L, 15000L, 21000L, 28000L};
        Object[] idleTarget = new Object[]{7000L, 28000L};
        ArrayList<Long> result = new ArrayList<>();
        ArrayList<Long> idleResult = new ArrayList<>();

        PrioritizedQueue<Long> queue = new UnlimitedQueue<>();
        QueuePoller<Long> p = new QueuePoller<>(l -> {
            result.add(Timeline.nowInUs());
            return l;
        }, queue, v -> idleResult.add(Timeline.nowInUs()));

        Consumer<Long> dropDataConsumer = l -> fail("Should not reach here!");

        Consumer<Object[]> addEventHandler = objs
                -> p.enQueue((Long) objs[0], (Boolean) objs[1], dropDataConsumer);

        Timeline.addEvent(0, addEventHandler, 1000L, false);
        Timeline.addEvent(0, addEventHandler, 2000L, true);
        Timeline.addEvent(0, addEventHandler, 3000L, false);
        Timeline.addEvent(0, addEventHandler, 4000L, true);
        Timeline.addEvent(0, addEventHandler, 5000L, false);
        Timeline.addEvent(15000, addEventHandler, 6000L, false);
        Timeline.addEvent(15000, addEventHandler, 7000L, true);
        Timeline.addEvent(15000, addEventHandler, 0L, false);
        Timeline.addEvent(4000, objs -> {
            @SuppressWarnings("unchecked")
            QueuePoller<Long> tmp = (QueuePoller<Long>) objs[0];
            @SuppressWarnings("unchecked")
            ArrayList<Long> tmp2 = new ArrayList<>();
            // at time 4000, 1000 and 2000 are done, 4000 running. remaining: 3000 and 5000
            assertStreamEquals(Stream.of(3000L, 5000L), tmp.stream());
            assertTrue(tmp.isBusy());
            tmp.clear(v -> tmp2.add(v));
            assertArrayEquals(new Object[]{3000L, 5000L}, tmp2.toArray());
            assertTrue(tmp.isBusy());
            assertStreamEquals(Stream.of(), tmp.stream());
            assertEquals(0, tmp.getSize());
        }, p);
        Timeline.addEvent(7000, objs -> assertTrue(((QueuePoller<Long>) objs[0]).isBusy()), p);
        Timeline.addEvent(7001, objs -> assertFalse(((QueuePoller<Long>) objs[0]).isBusy()), p);

        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());
        assertEquals(28000L, Timeline.run());
        assertArrayEquals(idleTarget, idleResult.toArray());
        assertArrayEquals(target, result.toArray());
        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());

    }

    @Test
    public void test3() {
        Object[] target = new Object[]{0L, 1000L, 3000L, 15000L, 21000L, 28000L};
        Object[] idleTarget = new Object[]{7000L, 28000L};
        ArrayList<Long> result = new ArrayList<>();
        ArrayList<Long> idleResult = new ArrayList<>();

        PrioritizedQueue<Long> queue = new UnlimitedQueue<>();
        QueuePoller<Long> p = new QueuePoller<>(l -> {
            result.add(Timeline.nowInUs());
            return l;
        }, queue, v -> idleResult.add(Timeline.nowInUs()));

        Consumer<Object[]> addEventHandler = objs
                -> p.enQueue((Long) objs[0], (Boolean) objs[1]);

        Timeline.addEvent(0, addEventHandler, 1000L, false);
        Timeline.addEvent(0, addEventHandler, 2000L, true);
        Timeline.addEvent(0, addEventHandler, 3000L, false);
        Timeline.addEvent(0, addEventHandler, 4000L, true);
        Timeline.addEvent(0, addEventHandler, 5000L, false);
        Timeline.addEvent(15000, addEventHandler, 6000L, false);
        Timeline.addEvent(15000, addEventHandler, 7000L, true);
        Timeline.addEvent(15000, addEventHandler, 0L, false);
        Timeline.addEvent(4000, objs -> {
            QueuePoller<Long> tmp = (QueuePoller<Long>) objs[0];
            // at time 4000, 1000 and 2000 are done, 4000 running. remaining: 3000 and 5000
            assertStreamEquals(Stream.of(3000L, 5000L), tmp.stream());
            assertTrue(tmp.isBusy());
            tmp.clear();
            assertTrue(tmp.isBusy());
            assertStreamEquals(Stream.of(), tmp.stream());
            assertEquals(0, tmp.getSize());
        }, p);
        Timeline.addEvent(7000, objs -> assertTrue(((QueuePoller<Long>) objs[0]).isBusy()), p);
        Timeline.addEvent(7001, objs -> assertFalse(((QueuePoller<Long>) objs[0]).isBusy()), p);

        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());
        assertEquals(28000L, Timeline.run());
        assertArrayEquals(idleTarget, idleResult.toArray());
        assertArrayEquals(target, result.toArray());
        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());

    }
}

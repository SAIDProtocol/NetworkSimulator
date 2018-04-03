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
        ArrayList<Long> result = new ArrayList<>();

        PrioritizedQueue<Long> queue = new UnlimitedQueue<>();
        QueuePoller<Long> p = new QueuePoller<>((d, t) -> {
            result.add(Timeline.nowInUs());
            return t;
        }, queue);
        RandomData tmp = new RandomData(1000);

        Consumer<Object[]> addEventHandler = objs
                -> p.enQueue(tmp.copy(), (Long) objs[0], (Boolean) objs[1]);

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
        assertArrayEquals(target, result.toArray());
        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());
    }

    @Test
    public void test2() {
        Object[] target = new Object[]{0L, 1000L, 3000L, 15000L, 21000L, 28000L};
        ArrayList<Long> result = new ArrayList<>();

        PrioritizedQueue<Long> queue = new UnlimitedQueue<>();
        QueuePoller<Long> p = new QueuePoller<>((d, t) -> {
            result.add(Timeline.nowInUs());
            return t;
        }, queue);

        Consumer<Object[]> addEventHandler = objs
                -> p.enQueue(new RandomData((Integer) objs[0]), (Long) objs[1], (Boolean) objs[2]);

        Timeline.addEvent(0, addEventHandler, 1000, 1000L, false);
        Timeline.addEvent(0, addEventHandler, 2000, 2000L, true);
        Timeline.addEvent(0, addEventHandler, 3000, 3000L, false);
        Timeline.addEvent(0, addEventHandler, 4000, 4000L, true);
        Timeline.addEvent(0, addEventHandler, 5000, 5000L, false);
        Timeline.addEvent(15000, addEventHandler, 1000, 6000L, false);
        Timeline.addEvent(15000, addEventHandler, 1000, 7000L, true);
        Timeline.addEvent(15000, addEventHandler, 1000, 0L, false);
        Timeline.addEvent(4000, objs -> {
            QueuePoller<Long> tmp = (QueuePoller<Long>) objs[0];
            // at time 4000, 1000 and 2000 are done, 4000 running. remaining: 3000 and 5000
            assertStreamEquals(tmp.stream().map(d -> d.getV2()), Stream.of(3000L, 5000L));
            assertTrue(tmp.isBusy());
            assertEquals(8000, tmp.getSizeInBits());
            assertEquals(8000, tmp.clear());
            assertTrue(tmp.isBusy());
            assertStreamEquals(tmp.stream(), Stream.of());
            assertEquals(0, tmp.getSizeInBits());
        }, p);
        Timeline.addEvent(7000, objs -> assertTrue(((QueuePoller) objs[0]).isBusy()), p);
        Timeline.addEvent(7001, objs -> assertFalse(((QueuePoller) objs[0]).isBusy()), p);

        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());
        assertEquals(28000L, Timeline.run());
        assertArrayEquals(target, result.toArray());
        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());

    }

}

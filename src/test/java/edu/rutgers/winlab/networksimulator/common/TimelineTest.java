package edu.rutgers.winlab.networksimulator.common;

import java.util.ArrayList;
import java.util.function.Consumer;
import org.junit.AfterClass;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Jiachen Chen
 */
public class TimelineTest {

//    private static final Logger LOG = Logger.getLogger(TimelineTest.class.getName());
    public TimelineTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Test
    public void test1() {
//        LOG.log(Level.INFO, "Testing if the same order is kept in the timeline when time is the same");
        Integer[] target = new Integer[]{0, 1, 2, 3, 4};
        long time = 1000;

        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());
        ArrayList<Integer> result = new ArrayList<>();
        Consumer<Object[]> consumer = objs -> {
            result.add((Integer) objs[0]);
        };
        for (Integer integer : target) {
            Timeline.addEvent(time, consumer, integer);
        }
        assertEquals(target.length, Timeline.getSize());
        assertTrue(result.isEmpty());
        assertEquals(time, Timeline.run());
        assertArrayEquals(result.toArray(), target);
        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());
    }

    @Test
    public void test2() {
//        LOG.log(Level.INFO, "Testing if the time order is kept");
        ArrayList<Tuple2<Long, Integer>> input = new ArrayList<>();
        input.add(new Tuple2<>(1005L, 4));
        input.add(new Tuple2<>(1002L, 0));
        input.add(new Tuple2<>(1004L, 3));
        input.add(new Tuple2<>(1003L, 1));
        input.add(new Tuple2<>(1003L, 2));
        Integer[] target = new Integer[]{0, 1, 2, 3, 4};
        long maxTime = 1005L;

        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());
        ArrayList<Integer> result = new ArrayList<>();
        Consumer<Object[]> consumer = objs -> {
            result.add((Integer) objs[0]);
        };
        input.forEach((tuple) -> {
            Timeline.addEvent(tuple.getV1(), consumer, tuple.getV2());
        });
        assertEquals(input.size(), Timeline.getSize());
        assertTrue(result.isEmpty());
        assertEquals(maxTime, Timeline.run());
        assertArrayEquals(result.toArray(), target);
        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());
    }

    private void testAddEventSameTime(Object... params) {
        int val = (Integer) params[0];
        long time = Timeline.nowInUs();
        ((ArrayList<Tuple3<Long, Integer, String>>) params[1]).add(new Tuple3<>(time, val, "S"));
        if (val > 0) {
            Timeline.addEvent(time, this::testAddEventSameTime, val - 1, params[1]);
        }
    }

    private void testAddEventLaterTime(Object... params) {
        int val = (Integer) params[0];
        long time = Timeline.nowInUs();
        ((ArrayList<Tuple3<Long, Integer, String>>) params[1]).add(new Tuple3<>(time, val, "L"));
        if (val > 0) {
            Timeline.addEvent(time + 1, this::testAddEventLaterTime, val - 1, params[1]);
            Timeline.addEvent(time, this::testAddEventSameTime, val - 1, params[1]);
        }
    }

    @Test
    public void test3() {
//        LOG.log(Level.INFO, "Testing dynamically add event");
        long maxTime = 1005L;
        ArrayList<Tuple3<Long, Integer, String>> target = new ArrayList<>();
        target.add(new Tuple3<>(1000L, 5, "L"));
        target.add(new Tuple3<>(1000L, 4, "S"));
        target.add(new Tuple3<>(1000L, 3, "S"));
        target.add(new Tuple3<>(1000L, 2, "S"));
        target.add(new Tuple3<>(1000L, 1, "S"));
        target.add(new Tuple3<>(1000L, 0, "S"));
        target.add(new Tuple3<>(1001L, 4, "L"));
        target.add(new Tuple3<>(1001L, 3, "S"));
        target.add(new Tuple3<>(1001L, 2, "S"));
        target.add(new Tuple3<>(1001L, 1, "S"));
        target.add(new Tuple3<>(1001L, 0, "S"));
        target.add(new Tuple3<>(1002L, 3, "L"));
        target.add(new Tuple3<>(1002L, 2, "S"));
        target.add(new Tuple3<>(1002L, 1, "S"));
        target.add(new Tuple3<>(1002L, 0, "S"));
        target.add(new Tuple3<>(1003L, 2, "L"));
        target.add(new Tuple3<>(1003L, 1, "S"));
        target.add(new Tuple3<>(1003L, 0, "S"));
        target.add(new Tuple3<>(1004L, 1, "L"));
        target.add(new Tuple3<>(1004L, 0, "S"));
        target.add(new Tuple3<>(1005L, 0, "L"));

        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());
        ArrayList<Tuple3<Long, Integer, String>> result = new ArrayList<>();
        Timeline.addEvent(1000, this::testAddEventLaterTime, 5, result);

        assertTrue(result.isEmpty());
        assertEquals(maxTime, Timeline.run());
//        result.forEach((tuple) -> {
//            LOG.log(Level.INFO, "Time: {0}, Val: {1}, Type: {2}", new Object[]{tuple.getV1(), tuple.getV2(), tuple.getV3()});
//        });
        assertEquals(result, target);
        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());
    }

    @Test()
    public void test4() {
        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());
        Consumer<Object[]> c = prams -> {
            assertEquals(1000, Timeline.nowInUs());
            try {
                Timeline.addEvent(999, objs -> {
                });
                fail("Should not reach here! You cannot add an event in the past");
            } catch (IllegalArgumentException e) {

            }
        };
        assertEquals(0, Timeline.getSize());
        Timeline.addEvent(1000, c);
        assertEquals(1, Timeline.getSize());
        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());
        assertEquals(1000, Timeline.run());
    }

    @Test()
    public void test5() {
        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());
        assertEquals(Long.MIN_VALUE, Timeline.run());
        assertEquals(Long.MIN_VALUE, Timeline.nowInUs());
    }
}

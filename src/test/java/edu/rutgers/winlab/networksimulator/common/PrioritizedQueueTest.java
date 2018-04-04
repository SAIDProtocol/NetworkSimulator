package edu.rutgers.winlab.networksimulator.common;

import static edu.rutgers.winlab.networksimulator.common.Helper.assertStreamEquals;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.AfterClass;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Jiachen Chen
 */
public class PrioritizedQueueTest {

//    private static final Logger LOG = Logger.getLogger(PrioritizedQueueTest.class.getName());
    public PrioritizedQueueTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Test
    public void testUnlimitedQueue1() {
        PrioritizedQueue<Integer> pq = new UnlimitedQueue<>();
        Object[] target = new Object[]{2, 4, 0, 1, 3};
        ArrayList<Integer> result = new ArrayList<>();

        Consumer<Integer> c = i -> result.add(i);

        pq.enQueue(0, false, c);
        assertTrue(result.isEmpty());
        pq.enQueue(1, false, c);
        assertTrue(result.isEmpty());
        pq.enQueue(2, true, c);
        assertTrue(result.isEmpty());
        pq.enQueue(3, false, c);
        assertTrue(result.isEmpty());
        pq.enQueue(4, true, c);
        assertTrue(result.isEmpty());

        assertEquals(target.length, pq.getSize());

        pq.stream().forEach(c);
        assertArrayEquals(target, result.toArray());
        result.clear();

        for (int i = 0; i < target.length; i++) {
            assertEquals(target[i], pq.deQueue());
            assertEquals(target.length - i - 1, pq.getSize());
        }
        assertNull(pq.deQueue());

    }

    @Test
    public void testUnlimitedQueue2() {
        PrioritizedQueue<Integer> pq = new UnlimitedQueue<>();
        Object[] target = new Object[]{2, 4, 0, 1, 3};
        ArrayList<Integer> result = new ArrayList<>();

        Consumer<Integer> c = i -> result.add(i);

        pq.enQueue(0, false, c);
        assertTrue(result.isEmpty());
        pq.enQueue(1, false, c);
        assertTrue(result.isEmpty());
        pq.enQueue(2, true, c);
        assertTrue(result.isEmpty());
        pq.enQueue(3, false, c);
        assertTrue(result.isEmpty());
        pq.enQueue(4, true, c);
        assertTrue(result.isEmpty());

        assertEquals(target.length, pq.getSize());

        pq.stream().forEach(c);
        assertArrayEquals(target, result.toArray());

        result.clear();
        assertTrue(result.isEmpty());
        pq.clear(i -> result.add(i));
        assertArrayEquals(target, result.toArray());
        assertEquals(0, pq.getSize());

        assertNull(pq.deQueue());
    }

    @Test
    public void testSizeLimitedQueue1() {
        SizeLimitedQueue<Tuple2<Data, Integer>> lq = new SizeLimitedQueue<>(4999, v -> v.getV1().getSizeInBits());
        assertEquals(4999, lq.getCapacityInBits());

        ArrayList<Tuple2<Data, Integer>> input = new ArrayList<>();
        input.add(new Tuple2<>(new RandomData(1000), 0));
        input.add(new Tuple2<>(new RandomData(1001), 1));
        input.add(new Tuple2<>(new RandomData(1002), 2));
        input.add(new Tuple2<>(new RandomData(1003), 3));
        input.add(new Tuple2<>(new RandomData(1004), 4));
        input.add(new Tuple2<>(new RandomData(1005), 5));
        input.add(new Tuple2<>(new RandomData(1990), 6));
        input.add(new Tuple2<>(new RandomData(1989), 7));

        ArrayList<Tuple2<Data, Integer>> tmp = new ArrayList<>();
        Consumer<Tuple2<Data, Integer>> c = v -> tmp.add(v);

        lq.enQueue(input.get(0), false, c);
        assertTrue(tmp.isEmpty());
        lq.enQueue(input.get(1), false, c);
        assertTrue(tmp.isEmpty());
        lq.enQueue(input.get(2), true, c);
        assertTrue(tmp.isEmpty());
        lq.enQueue(input.get(3), true, c);
        assertTrue(tmp.isEmpty());
        assertStreamEquals(Stream.of(input.get(2), input.get(3), input.get(0), input.get(1)), lq.stream());
        assertEquals(4, lq.getSize());
        assertEquals(4006, lq.getSizeInBits());

        lq.enQueue(input.get(4), false, c);
        assertStreamEquals(Stream.of(input.get(4)), tmp.stream());
        assertStreamEquals(Stream.of(input.get(2), input.get(3), input.get(0), input.get(1)), lq.stream());
        assertEquals(4, lq.getSize());
        assertEquals(4006, lq.getSizeInBits());

        tmp.clear();
        assertTrue(tmp.isEmpty());

        lq.enQueue(input.get(5), true, c);
        assertStreamEquals(Stream.of(input.get(1)), tmp.stream());
        assertStreamEquals(Stream.of(input.get(2), input.get(3), input.get(5), input.get(0)), lq.stream());
        assertEquals(4, lq.getSize());
        assertEquals(4010, lq.getSizeInBits());

        tmp.clear();
        assertTrue(tmp.isEmpty());

        lq.enQueue(input.get(6), true, c);
        assertStreamEquals(Stream.of(input.get(0), input.get(6)), tmp.stream());
        assertStreamEquals(Stream.of(input.get(2), input.get(3), input.get(5)), lq.stream());
        assertEquals(3, lq.getSize());
        assertEquals(3010, lq.getSizeInBits());

        tmp.clear();
        assertTrue(tmp.isEmpty());

        lq.enQueue(input.get(7), true, c);
        assertTrue(tmp.isEmpty());
        assertStreamEquals(Stream.of(input.get(2), input.get(3), input.get(5), input.get(7)), lq.stream());
        assertEquals(4, lq.getSize());
        assertEquals(4999, lq.getSizeInBits());

        lq.clear(c);
        assertStreamEquals(Stream.of(input.get(2), input.get(3), input.get(5), input.get(7)), tmp.stream());
        assertStreamEquals(Stream.of(), lq.stream());
        assertEquals(0, lq.getSize());
        assertEquals(0, lq.getSizeInBits());

    }

    @Test
    public void testSizeLimitedQueue2() {
        SizeLimitedQueue<Tuple2<Data, Integer>> lq = new SizeLimitedQueue<>(4999, v -> v.getV1().getSizeInBits());
        assertEquals(4999, lq.getCapacityInBits());

        ArrayList<Tuple2<Data, Integer>> input = new ArrayList<>();
        input.add(new Tuple2<>(new RandomData(1000), 0));
        input.add(new Tuple2<>(new RandomData(1001), 1));
        input.add(new Tuple2<>(new RandomData(1002), 2));
        input.add(new Tuple2<>(new RandomData(1003), 3));
        input.add(new Tuple2<>(new RandomData(1004), 4));
        input.add(new Tuple2<>(new RandomData(1005), 5));
        input.add(new Tuple2<>(new RandomData(1990), 6));
        input.add(new Tuple2<>(new RandomData(1989), 7));

        ArrayList<Tuple2<Data, Integer>> tmp = new ArrayList<>();

        lq.enQueue(input.get(0), false);
        lq.enQueue(input.get(1), false);
        lq.enQueue(input.get(2), true);
        lq.enQueue(input.get(3), true);
        assertStreamEquals(Stream.of(input.get(2), input.get(3), input.get(0), input.get(1)), lq.stream());
        assertEquals(4, lq.getSize());
        assertEquals(4006, lq.getSizeInBits());

        lq.enQueue(input.get(4), false);
        assertStreamEquals(Stream.of(input.get(2), input.get(3), input.get(0), input.get(1)), lq.stream());
        assertEquals(4, lq.getSize());
        assertEquals(4006, lq.getSizeInBits());

        lq.enQueue(input.get(5), true);
        assertStreamEquals(Stream.of(input.get(2), input.get(3), input.get(5), input.get(0)), lq.stream());
        assertEquals(4, lq.getSize());
        assertEquals(4010, lq.getSizeInBits());

        lq.enQueue(input.get(6), true);
        assertStreamEquals(Stream.of(input.get(2), input.get(3), input.get(5)), lq.stream());
        assertEquals(3, lq.getSize());
        assertEquals(3010, lq.getSizeInBits());

        lq.enQueue(input.get(7), true);
        assertStreamEquals(Stream.of(input.get(2), input.get(3), input.get(5), input.get(7)), lq.stream());
        assertEquals(4, lq.getSize());
        assertEquals(4999, lq.getSizeInBits());

        lq.clear();
        assertStreamEquals(Stream.of(), lq.stream());
        assertEquals(0, lq.getSize());
        assertEquals(0, lq.getSizeInBits());

        assertNull(lq.deQueue());
    }

    @Test
    public void testSizeLimitedQueue3() {
        SizeLimitedQueue<Tuple2<Data, Integer>> lq = new SizeLimitedQueue<>(4999, v -> v.getV1().getSizeInBits());
        assertEquals(4999, lq.getCapacityInBits());

        ArrayList<Tuple2<Data, Integer>> input = new ArrayList<>();
        input.add(new Tuple2<>(new RandomData(1000), 0));
        input.add(new Tuple2<>(new RandomData(1001), 1));
        input.add(new Tuple2<>(new RandomData(1002), 2));
        input.add(new Tuple2<>(new RandomData(1003), 3));
        input.add(new Tuple2<>(new RandomData(1004), 4));
        input.add(new Tuple2<>(new RandomData(1005), 5));
        input.add(new Tuple2<>(new RandomData(1990), 6));
        input.add(new Tuple2<>(new RandomData(1989), 7));

        ArrayList<Tuple2<Data, Integer>> tmp = new ArrayList<>();

        lq.enQueue(input.get(0), false);
        lq.enQueue(input.get(1), false);
        lq.enQueue(input.get(2), true);
        lq.enQueue(input.get(3), true);
        assertStreamEquals(Stream.of(input.get(2), input.get(3), input.get(0), input.get(1)), lq.stream());
        assertEquals(4, lq.getSize());
        assertEquals(4006, lq.getSizeInBits());

        lq.enQueue(input.get(4), false);
        assertStreamEquals(Stream.of(input.get(2), input.get(3), input.get(0), input.get(1)), lq.stream());
        assertEquals(4, lq.getSize());
        assertEquals(4006, lq.getSizeInBits());

        lq.enQueue(input.get(5), true);
        assertStreamEquals(Stream.of(input.get(2), input.get(3), input.get(5), input.get(0)), lq.stream());
        assertEquals(4, lq.getSize());
        assertEquals(4010, lq.getSizeInBits());

        lq.enQueue(input.get(6), true);
        assertStreamEquals(Stream.of(input.get(2), input.get(3), input.get(5)), lq.stream());
        assertEquals(3, lq.getSize());
        assertEquals(3010, lq.getSizeInBits());

        lq.enQueue(input.get(7), true);
        assertStreamEquals(Stream.of(input.get(2), input.get(3), input.get(5), input.get(7)), lq.stream());
        assertEquals(4, lq.getSize());
        assertEquals(4999, lq.getSizeInBits());

        while (lq.getSize() > 0) {
            tmp.add(lq.deQueue());
        }
        assertStreamEquals(Stream.of(input.get(2), input.get(3), input.get(5), input.get(7)), tmp.stream());
        assertStreamEquals(Stream.of(), lq.stream());
        assertEquals(0, lq.getSize());
        assertEquals(0, lq.getSizeInBits());

        assertNull(lq.deQueue());
    }

    @Test
    public void testItemLimitedQueue1() {
        ItemLimitedQueue<Integer> pq = new ItemLimitedQueue<>(3);
        assertEquals(3, pq.getCapacityInItems());
        Object[] target = new Object[]{0, 2, 4};
        ArrayList<Integer> result = new ArrayList<>();

        Consumer<Integer> c = i -> result.add(i);

        pq.enQueue(0, true, c);
        assertTrue(result.isEmpty());
        pq.enQueue(1, false, c);
        assertTrue(result.isEmpty());
        pq.enQueue(2, true, c);
        assertTrue(result.isEmpty());
        pq.enQueue(3, false, c);
        assertStreamEquals(Stream.of(3), result.stream());
        result.clear();
        assertTrue(result.isEmpty());

        pq.enQueue(4, true, c);
        assertStreamEquals(Stream.of(1), result.stream());
        result.clear();
        assertTrue(result.isEmpty());

        pq.enQueue(5, true, c);
        assertStreamEquals(Stream.of(5), result.stream());
        result.clear();
        assertTrue(result.isEmpty());

        assertEquals(target.length, pq.getSize());

        pq.stream().forEach(c);
        assertArrayEquals(target, result.toArray());
        result.clear();

        for (int i = 0; i < target.length; i++) {
            assertEquals(target[i], pq.deQueue());
            assertEquals(target.length - i - 1, pq.getSize());
        }
        assertNull(pq.deQueue());

    }

    @Test
    public void testItemLimitedQueue2() {
        ItemLimitedQueue<Integer> pq = new ItemLimitedQueue<>(3);
        assertEquals(3, pq.getCapacityInItems());
        Object[] target = new Object[]{0, 2, 4};
        ArrayList<Integer> result = new ArrayList<>();

        pq.enQueue(0, true);
        assertStreamEquals(Stream.of(0), pq.stream());
        pq.enQueue(1, false);
        assertStreamEquals(Stream.of(0, 1), pq.stream());
        pq.enQueue(2, true);
        assertStreamEquals(Stream.of(0, 2, 1), pq.stream());
        pq.enQueue(3, false);
        assertStreamEquals(Stream.of(0, 2, 1), pq.stream());
        pq.enQueue(4, true);
        assertStreamEquals(Stream.of(0, 2, 4), pq.stream());
        pq.enQueue(5, true);
        assertStreamEquals(Stream.of(0, 2, 4), pq.stream());

        assertEquals(target.length, pq.getSize());

        pq.stream().forEach(d -> result.add(d));
        assertArrayEquals(target, result.toArray());
        result.clear();

        for (int i = 0; i < target.length; i++) {
            assertEquals(target[i], pq.deQueue());
            assertEquals(target.length - i - 1, pq.getSize());
        }
        assertNull(pq.deQueue());

    }
}

package edu.rutgers.winlab.networksimulator.common;

import static edu.rutgers.winlab.networksimulator.common.Helper.assertStreamEquals;
import java.util.ArrayList;
import java.util.stream.Stream;
import org.junit.AfterClass;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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

    private class RandomData implements Data {

        private final int sizeInBits;
        private final int id;

        public RandomData(int sizeInBits, int id) {
            this.sizeInBits = sizeInBits;
            this.id = id;
        }

        public int getId() {
            return id;
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
        PrioritizedQueue pq = new UnlimitedQueue();
        Object[] target = new Object[]{2, 4, 0, 1, 3};
        int size = 1000;
        ArrayList<Integer> result = new ArrayList<>();

        assertEquals(0, pq.enQueue(new RandomData(size, 0), false));
        assertEquals(0, pq.enQueue(new RandomData(size, 1), false));
        assertEquals(0, pq.enQueue(new RandomData(size, 2), true));
        assertEquals(0, pq.enQueue(new RandomData(size, 3), false));
        assertEquals(0, pq.enQueue(new RandomData(size, 4), true));

        assertEquals(size * target.length, pq.getSizeInBits());

        pq.stream().forEach(d -> result.add(((RandomData) d).id));
        assertArrayEquals(result.toArray(), target);
        result.clear();

        for (int i = 0; i < target.length; i++) {
            assertEquals(target[i], ((RandomData) pq.deQueue()).id);
            assertEquals(size * (target.length - i - 1), pq.getSizeInBits());
        }
        assertNull(pq.deQueue());

    }

    @Test
    public void test2() {
        PrioritizedQueue pq = new UnlimitedQueue();
        Object[] target = new Object[]{2, 4, 0, 1, 3};
        int size = 1000;
        ArrayList<Integer> result = new ArrayList<>();

        assertEquals(0, pq.enQueue(new RandomData(size, 0), false));
        assertEquals(0, pq.enQueue(new RandomData(size, 1), false));
        assertEquals(0, pq.enQueue(new RandomData(size, 2), true));
        assertEquals(0, pq.enQueue(new RandomData(size, 3), false));
        assertEquals(0, pq.enQueue(new RandomData(size, 4), true));

        assertEquals(size * target.length, pq.getSizeInBits());

        pq.stream().forEach(d -> result.add(((RandomData) d).id));
        assertArrayEquals(result.toArray(), target);

        assertEquals(size * target.length, pq.clear());
        assertNull(pq.deQueue());

    }

    @Test
    public void test3() {
        LimitedQueue lq = new LimitedQueue(4999);
        assertEquals(0, lq.enQueue(new RandomData(1000, 0), false));
        assertEquals(0, lq.enQueue(new RandomData(1001, 1), false));
        assertEquals(0, lq.enQueue(new RandomData(1002, 2), true));
        assertEquals(0, lq.enQueue(new RandomData(1003, 3), true));
        assertStreamEquals(Stream.of(2, 3, 0, 1), lq.stream().map(d -> ((RandomData) d).id));
        assertEquals(4006, lq.getSizeInBits());

        assertEquals(1004, lq.enQueue(new RandomData(1004, 4), false));
        assertStreamEquals(lq.stream().map(d -> ((RandomData) d).id), Stream.of(2, 3, 0, 1));
        assertEquals(4006, lq.getSizeInBits());

        assertEquals(1001, lq.enQueue(new RandomData(1005, 5), true));
        assertStreamEquals(lq.stream().map(d -> ((RandomData) d).id), Stream.of(2, 3, 5, 0));
        assertEquals(4010, lq.getSizeInBits());

        assertEquals(2990, lq.enQueue(new RandomData(1990, 6), true));
        assertStreamEquals(lq.stream().map(d -> ((RandomData) d).id), Stream.of(2, 3, 5));
        assertEquals(3010, lq.getSizeInBits());

        assertEquals(0, lq.enQueue(new RandomData(1989, 7), false));
        assertStreamEquals(lq.stream().map(d -> ((RandomData) d).id), Stream.of(2, 3, 5, 7));
        assertEquals(4999, lq.getSizeInBits());
    }
}

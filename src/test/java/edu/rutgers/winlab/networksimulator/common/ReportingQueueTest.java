/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.networksimulator.common;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jiachen
 */
public class ReportingQueueTest {

    public ReportingQueueTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Test
    public void testSomeMethod() {
        String queueName = "Test";

        ItemLimitedQueue<Integer> q = new ItemLimitedQueue<>(2);
        Tuple1<Integer> tmp1 = new Tuple1<>(0);
        int[] queueSizes = new int[]{1, 2, 1, 0, 1, 2, 0, 1, 2, 0, 1, 2, 2, 2};
        Integer[] contentsInQueue0 = new Integer[0];
        Integer[] contentsInQueue1 = new Integer[]{3};
        Integer[] contentsInQueue2 = new Integer[]{4, 3};
        Integer[] contentsInQueue3 = new Integer[]{4, 5};

        BiConsumer<String, Integer> sizeChangeHandler = (x, i) -> {
            assertEquals(x, queueName);
            assertEquals((long) queueSizes[tmp1.getV1()], (long) i);
            tmp1.mergeV1(1, Integer::sum);
        };
        ReportingQueue<Integer, UnlimitedQueue<Integer>> rq = new ReportingQueue<>(queueName, q, sizeChangeHandler);
        assertEquals(rq.getName(), queueName);

        assertArrayEquals(rq.stream().toArray(), contentsInQueue0);
        rq.enQueue(3, false);
        assertArrayEquals(rq.stream().toArray(), contentsInQueue1);
        assertEquals(rq.getSize(), 1);
        rq.enQueue(4, true);
        assertArrayEquals(rq.stream().toArray(), contentsInQueue2);
        assertEquals(rq.getSize(), 2);
        assertEquals((long) rq.deQueue(), 4);
        assertArrayEquals(rq.stream().toArray(), contentsInQueue1);
        assertEquals(rq.getSize(), 1);
        assertEquals((long) rq.deQueue(), 3);
        assertArrayEquals(rq.stream().toArray(), contentsInQueue0);
        assertEquals(rq.getSize(), 0);

        rq.enQueue(3, false);
        assertArrayEquals(rq.stream().toArray(), contentsInQueue1);
        assertEquals(rq.getSize(), 1);
        rq.enQueue(4, true);
        assertArrayEquals(rq.stream().toArray(), contentsInQueue2);
        assertEquals(rq.getSize(), 2);
        rq.clear();
        assertArrayEquals(rq.stream().toArray(), contentsInQueue0);
        assertEquals(rq.getSize(), 0);

        Tuple1<Integer> tmp2 = new Tuple1<>(0);
        Consumer<Integer> clearConsumer = i -> {
            assertEquals((long) contentsInQueue2[tmp2.getV1()], (long) i);
            tmp2.mergeV1(1, Integer::sum);
        };

        rq.enQueue(3, false);
        assertArrayEquals(rq.stream().toArray(), contentsInQueue1);
        assertEquals(rq.getSize(), 1);
        rq.enQueue(4, true);
        assertArrayEquals(rq.stream().toArray(), contentsInQueue2);
        assertEquals(rq.getSize(), 2);
        rq.clear(clearConsumer);
        assertEquals((long) tmp2.getV1(), contentsInQueue2.length);

        Consumer<Integer> failConsumer = i -> fail("Should not be called!");
        Tuple1<Integer> testVal = new Tuple1<>(0);
        Consumer<Integer> confirmConsumer = i -> assertEquals(i, testVal.getV1());

        rq.enQueue(3, false, failConsumer);
        assertArrayEquals(rq.stream().toArray(), contentsInQueue1);
        assertEquals(rq.getSize(), 1);
        rq.enQueue(4, true, failConsumer);
        assertArrayEquals(rq.stream().toArray(), contentsInQueue2);
        assertEquals(rq.getSize(), 2);
        testVal.setV1(5);
        rq.enQueue(5, false, confirmConsumer); // cannot input, spit out 5
        assertArrayEquals(rq.stream().toArray(), contentsInQueue2);
        assertEquals(rq.getSize(), 2);
        testVal.setV1(3);
        rq.enQueue(5, true, confirmConsumer); // input and, spit out 5
        assertArrayEquals(rq.stream().toArray(), contentsInQueue3);
        assertEquals(rq.getSize(), 2);

        assertEquals((long) tmp1.getV1(), queueSizes.length);

    }

}

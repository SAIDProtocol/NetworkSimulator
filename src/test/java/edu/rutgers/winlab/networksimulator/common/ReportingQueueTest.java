/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.networksimulator.common;

import java.util.function.BiConsumer;
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

        UnlimitedQueue<Integer> q = new UnlimitedQueue<>();

        BiConsumer<String, Integer> consumer = (x, i) -> {
            System.out.printf("q=%s,i=%d%n", x, i);
        };
        ReportingQueue<Integer, UnlimitedQueue<Integer>> rq = new ReportingQueue<>("Test", q, consumer);

        rq.enQueue(3, false);
        rq.enQueue(4, true);
        System.out.printf("get: %d%n", rq.deQueue());
        System.out.printf("get: %d%n", rq.deQueue());
    }

}

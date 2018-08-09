/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.networksimulator.common;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jiachen
 */
public class ReportObjectTest {

    public ReportObjectTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    public static final class DevNull {

        public final static PrintStream out = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
            }
        });
    }

    @Test
    public void testSomeMethod() throws InterruptedException {
        ReportObject ro = new ReportObject();
        ro.setWriter(DevNull.out);
        ro.setKey("v1", 0);
        ro.setKey("v2", 1);
        ro.setKey("time", () -> new Date().toString());
        ro.beginReport();
        try {
            ro.beginReport();
            fail("Should not be able to begin report more than once.");
        } catch (Throwable e) {
        }
        ro.setValue(0, 15);
        for (int i = 0; i < 10; i++) {
            ro.incrementValue(0);
            ro.computeValue(1, (k, v) -> (k + v + 1));
            Thread.sleep(300);
        }
        assertEquals(ro.getValue(0), 25);
        assertEquals(ro.getValue(1), 20);

        ro.endReport();
        try {
            ro.endReport();
            fail("Should not be able to begin report more than once.");
        } catch (Throwable e) {
        }
        ro.beginReport();
        Thread.sleep(1000);
        ro.endReport();
        assertEquals(ro.getWriter(), DevNull.out);
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.networksimulator.network.mf.scdn;

import edu.rutgers.winlab.networksimulator.common.Timeline;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jiachen
 */
public class TraceGeneratorTest {

    public TraceGeneratorTest() {
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

//    @Test
    public void testSomeMethod() {
        double s = 1.0;
        int total = 10000, views = total * 10;
        Random rand = new Random(0);
        long minTime = 30 * Timeline.SECOND, maxTime = 2 * Timeline.HOUR;

        Function<Integer, Long> videoLengthGenerator = TraceGenerator.toIntFunction(
                TraceGenerator.addFilter(
                        TraceGenerator.getNormalDistributionGenerator(rand, 0.5, 1.0 / 6),
                        d -> d >= 0 && d < 1)
        ).andThen(TraceGenerator.getDoubleToLongFunction(minTime, maxTime));

        Function<Integer, Long> videoPopularityGenerator
                = TraceGenerator.getZipfDistributionGenerator(s, total)
                        .andThen(d -> Math.round(d * views));

        TraceGenerator.generateTrace(rand, total, videoLengthGenerator, videoPopularityGenerator, null);

    }

}

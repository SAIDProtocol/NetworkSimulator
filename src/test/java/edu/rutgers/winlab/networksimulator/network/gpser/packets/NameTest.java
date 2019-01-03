/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.networksimulator.network.gpser.packets;

import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ubuntu
 */
public class NameTest {

    public NameTest() {
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

    @Test
    public void test1() {
        int[] parents = new int[]{3, 3, 5, 7, 7, 8, 11, 11, 11};
        int[] children = new int[]{8, 10, 11, 8, 11, 9, 2, 9, 10};
        for (int i = 0; i < parents.length; i++) {
            Name.connectNames(Name.getName(parents[i]), Name.getName(children[i]));
        }
        Name.getExistingNames().forEach(n
                -> System.out.printf("%s: %s%n", n, Arrays.toString(n.getChildren().toArray()))
        );
        System.out.println(Arrays.toString(Name.topologicalSort().toArray()));
        Name.getExistingNames().forEach(n
                -> System.out.printf("%s: %s%n", n, Arrays.toString(n.getDescendents().toArray()))
        );
        Name.getExistingNames().forEach(n
                -> System.out.printf("%s: %s%n", n, Arrays.toString(n.getAncestors().toArray()))
        );

    }

}

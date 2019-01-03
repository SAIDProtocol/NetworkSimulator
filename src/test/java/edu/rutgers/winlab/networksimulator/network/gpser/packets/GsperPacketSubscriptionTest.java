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
public class GsperPacketSubscriptionTest {

    public GsperPacketSubscriptionTest() {
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
        GpserPacketSubscription subscription = new GpserPacketSubscription();
        subscription.subscribe(Name.getName(1));    // add subscription
        System.out.println(Arrays.toString(subscription.getSubscriptions().toArray()));
        subscription.subscribe(Name.getName(1));    // redundant, warning
        System.out.println(Arrays.toString(subscription.getSubscriptions().toArray()));
        subscription.unsubscribe(Name.getName(1));  // cancel subscription
        System.out.println(Arrays.toString(subscription.getSubscriptions().toArray()));
        subscription.unsubscribe(Name.getName(1));  // unsubscribe
        System.out.println(Arrays.toString(subscription.getSubscriptions().toArray()));
        subscription.unsubscribe(Name.getName(1));  // redundant, warning
        System.out.println(Arrays.toString(subscription.getSubscriptions().toArray()));

    }

}

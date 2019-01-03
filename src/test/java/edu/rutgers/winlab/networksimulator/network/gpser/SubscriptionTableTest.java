/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.networksimulator.network.gpser;

import edu.rutgers.winlab.networksimulator.network.gpser.packets.GpserPacketSubscription;
import edu.rutgers.winlab.networksimulator.network.gpser.packets.Name;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author ubuntu
 */
public class SubscriptionTableTest {

    public SubscriptionTableTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        Name.connectNames(Name.getName(1), Name.getName(4));
        Name.connectNames(Name.getName(2), Name.getName(4));
        Name.connectNames(Name.getName(2), Name.getName(5));
        Name.connectNames(Name.getName(3), Name.getName(5));
        Name.connectNames(Name.getName(5), Name.getName(6));
        Name.connectNames(Name.getName(7), Name.getName(5));

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

    private void printSubscriptionTable(SubscriptionTable<String> sub) {
        sub.getOutgoingInterfaces().forEach(t -> System.out.printf("O %s->%s%n", t.getV1(), Arrays.toString(t.getV2().toArray())));
        sub.getCoveredBy().forEach(t -> System.out.printf("C %s->%s%n", t.getV1(), Arrays.toString(t.getV2().toArray())));
        Name.getExistingNames().forEach(n -> System.out.printf("F %s->%s%n", n, Arrays.toString(sub.getNameInterfaces(n).toArray())));
    }

    private void subscribe(Name n, String face, SubscriptionTable<String> sub1, SubscriptionTable<String> sub2) {
        System.out.printf("===Sub %s===%n", n);
        GpserPacketSubscription sub = new GpserPacketSubscription();
        sub.subscribe(n);
        System.out.printf("Before sub1: sub=%s%n", Arrays.toString(sub.getSubscriptions().toArray()));
        sub = sub1.handleSubscription(sub, face);
        System.out.printf("After sub1: sub=%s%n", Arrays.toString(sub.getSubscriptions().toArray()));
        System.out.println("---Sub1---");
        printSubscriptionTable(sub1);
        sub = sub2.handleSubscription(sub, "sub1");
        System.out.printf("After sub2: sub=%s%n", Arrays.toString(sub.getSubscriptions().toArray()));
        System.out.println("---Sub2---");
        printSubscriptionTable(sub2);
    }
    
    private void unSubscribe(Name n, String face, SubscriptionTable<String> sub1, SubscriptionTable<String> sub2) {
        System.out.printf("===UnSub %s===%n", n);
        GpserPacketSubscription sub = new GpserPacketSubscription();
        sub.unsubscribe(n);
        System.out.printf("Before sub1: sub=%s%n", Arrays.toString(sub.getSubscriptions().toArray()));
        sub = sub1.handleSubscription(sub, face);
        System.out.printf("After sub1: sub=%s%n", Arrays.toString(sub.getSubscriptions().toArray()));
        System.out.println("---Sub1---");
        printSubscriptionTable(sub1);
        sub = sub2.handleSubscription(sub, "sub1");
        System.out.printf("After sub2: sub=%s%n", Arrays.toString(sub.getSubscriptions().toArray()));
        System.out.println("---Sub2---");
        printSubscriptionTable(sub2);
    }    

    @Test
    public void test1() {
        SubscriptionTable<String> sub1 = new SubscriptionTable<>();
        SubscriptionTable<String> sub2 = new SubscriptionTable<>();
        subscribe(Name.getName(1), "A", sub1, sub2);
        subscribe(Name.getName(2), "B", sub1, sub2);
        subscribe(Name.getName(3), "C", sub1, sub2);
        subscribe(Name.getName(4), "D", sub1, sub2);
        subscribe(Name.getName(5), "E", sub1, sub2);
        subscribe(Name.getName(6), "F", sub1, sub2);
        subscribe(Name.getName(7), "G", sub1, sub2);

        unSubscribe(Name.getName(7), "G", sub1, sub2);
        unSubscribe(Name.getName(6), "F", sub1, sub2);
        unSubscribe(Name.getName(5), "E", sub1, sub2);
        unSubscribe(Name.getName(4), "D", sub1, sub2);
        unSubscribe(Name.getName(3), "C", sub1, sub2);
        unSubscribe(Name.getName(2), "B", sub1, sub2);
        unSubscribe(Name.getName(1), "A", sub1, sub2);
    }

}

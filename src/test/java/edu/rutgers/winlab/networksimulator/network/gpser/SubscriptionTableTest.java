/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.networksimulator.network.gpser;

import edu.rutgers.winlab.networksimulator.network.gpser.packets.GpserPacketSubscription;
import edu.rutgers.winlab.networksimulator.network.gpser.packets.Name;
import java.util.Arrays;
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
        Name.clearExistingNames();
        Name.connectNames(Name.getName(1), Name.getName(4));
        Name.connectNames(Name.getName(2), Name.getName(4));
        Name.connectNames(Name.getName(2), Name.getName(5));
        Name.connectNames(Name.getName(3), Name.getName(5));
        Name.connectNames(Name.getName(5), Name.getName(6));
        Name.connectNames(Name.getName(7), Name.getName(5));
    }

    @AfterClass
    public static void tearDownClass() {
        Name.clearExistingNames();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private <T> void printSubscriptionTable(SubscriptionTable<T> sub) {
        sub.getOutgoingInterfaces().forEach(t -> System.out.printf("O %s->%s%n", t.getV1(), Arrays.toString(t.getV2().toArray())));
        sub.getCoveredBy().forEach(t -> System.out.printf("C %s->%s%n", t.getV1(), Arrays.toString(t.getV2().toArray())));
        Name.getExistingNames().forEach(n -> System.out.printf("F %s->%s%n", n, Arrays.toString(sub.getNameInterfaces(n).toArray())));
    }

    private <T> void subscribe(SubscriptionTable<T> sub1, T face1, SubscriptionTable<T> sub2, T face2, Object... params) {
        if (params.length % 2 != 0) {
            throw new IllegalArgumentException("params should follow %name %sub/unsub format");
        }
        GpserPacketSubscription sub = new GpserPacketSubscription();
        for (int i = 0; i < params.length / 2; i++) {
            if (!(params[i * 2] instanceof Name) || !(params[i * 2 + 1] instanceof Boolean)) {
                throw new IllegalArgumentException("params should follow %name %sub/unsub format, error at set " + i);
            }
            if ((Boolean) params[i * 2 + 1]) {
                sub.subscribe((Name) params[i * 2]);
            } else {
                sub.unsubscribe((Name) params[i * 2]);
            }
        }
        System.out.printf("======%s======%n", Arrays.toString(sub.getSubscriptions().toArray()));
        sub = sub1.handleSubscription(sub, face1);
        System.out.printf("After sub1: sub=%s%n", Arrays.toString(sub.getSubscriptions().toArray()));
        System.out.println("---Sub1---");
        printSubscriptionTable(sub1);
        sub = sub2.handleSubscription(sub, face2);
        System.out.printf("After sub2: sub=%s%n", Arrays.toString(sub.getSubscriptions().toArray()));
        System.out.println("---Sub2---");
        printSubscriptionTable(sub2);

    }

    @Test
    public void test1() {
        SubscriptionTable<String> sub1 = new SubscriptionTable<>();
        SubscriptionTable<String> sub2 = new SubscriptionTable<>();
        subscribe(sub1, "A", sub2, "sub1", Name.getName(1), true);
        subscribe(sub1, "B", sub2, "sub1", Name.getName(2), true);
        subscribe(sub1, "C", sub2, "sub1", Name.getName(3), true);
        subscribe(sub1, "D", sub2, "sub1", Name.getName(4), true);
        subscribe(sub1, "E", sub2, "sub1", Name.getName(5), true);
        subscribe(sub1, "F", sub2, "sub1", Name.getName(6), true);
        subscribe(sub1, "G", sub2, "sub1", Name.getName(7), true);

        subscribe(sub1, "A2", sub2, "sub1", Name.getName(1), true);
        subscribe(sub1, "B2", sub2, "sub1", Name.getName(2), true);
        subscribe(sub1, "C2", sub2, "sub1", Name.getName(3), true);
        subscribe(sub1, "D2", sub2, "sub1", Name.getName(4), true);
        subscribe(sub1, "E2", sub2, "sub1", Name.getName(5), true);
        subscribe(sub1, "F2", sub2, "sub1", Name.getName(6), true);
        subscribe(sub1, "G2", sub2, "sub1", Name.getName(7), true);

        subscribe(sub1, "G", sub2, "sub1", Name.getName(7), false);
        subscribe(sub1, "F", sub2, "sub1", Name.getName(6), false);
        subscribe(sub1, "E", sub2, "sub1", Name.getName(5), false);
        subscribe(sub1, "D", sub2, "sub1", Name.getName(4), false);
        subscribe(sub1, "C", sub2, "sub1", Name.getName(3), false);
        subscribe(sub1, "B", sub2, "sub1", Name.getName(2), false);
        subscribe(sub1, "A", sub2, "sub1", Name.getName(1), false);

        subscribe(sub1, "G2", sub2, "sub1", Name.getName(7), false);
        subscribe(sub1, "F2", sub2, "sub1", Name.getName(6), false);
        subscribe(sub1, "E2", sub2, "sub1", Name.getName(5), false);
        subscribe(sub1, "D2", sub2, "sub1", Name.getName(4), false);
        subscribe(sub1, "C2", sub2, "sub1", Name.getName(3), false);
        subscribe(sub1, "B2", sub2, "sub1", Name.getName(2), false);
        subscribe(sub1, "A2", sub2, "sub1", Name.getName(1), false);
    }

    @Test
    public void test2() {
        SubscriptionTable<String> sub1 = new SubscriptionTable<>();
        SubscriptionTable<String> sub2 = new SubscriptionTable<>();
        subscribe(sub1, "A", sub2, "sub1", Name.getName(1), true);
        subscribe(sub1, "A", sub2, "sub1", Name.getName(2), true);
        subscribe(sub1, "A", sub2, "sub1", Name.getName(3), true);
        subscribe(sub1, "A", sub2, "sub1", Name.getName(4), true);
        subscribe(sub1, "A", sub2, "sub1", Name.getName(5), true);
        subscribe(sub1, "A", sub2, "sub1", Name.getName(6), true);
        subscribe(sub1, "A", sub2, "sub1", Name.getName(7), true);

        subscribe(sub1, "A", sub2, "sub1", Name.getName(6), false, Name.getName(5), false);
    }

}

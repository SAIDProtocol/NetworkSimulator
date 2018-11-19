/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.networksimulator.network.mf.graphpubsub;

import edu.rutgers.winlab.networksimulator.common.ReportObject;
import edu.rutgers.winlab.networksimulator.common.Timeline;
import edu.rutgers.winlab.networksimulator.common.Tuple2;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author root
 */
public class ResultHandler {

    public ResultHandler() {
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
    public void readQueue() throws IOException {
        String folderName = "./";
        File folder = new File(folderName);
        File[] files = folder.listFiles((dir, name) -> {
            return name.matches("queue_.*\\.txt");
        });
        for (File file : files) {
//            System.out.printf("%s%n", file.toString());
            Tuple2<Long, Integer> t = new Tuple2<>(0L, 0);
            AtomicInteger lineCount = new AtomicInteger();
            Files.lines(Paths.get(file.toURI())).forEach(line -> {
                int l = lineCount.incrementAndGet();
                String[] parts = line.split("\t");
                assert parts.length == 2;
                long time = Long.parseLong(parts[0]);
                int size = Integer.parseInt(parts[1]);
                if (size > t.getV2()) {
                    t.setValues(time, size);
//                    System.out.printf("%d, %d\t%d%n", l, time, size);
                }
            });
            System.out.printf("%s\t%d\t%d%n", file.getName(), t.getV1(), t.getV2());
        }
    }

    static class Delivery {

        public enum DeliveryState {
            Deliver, Finish, Redundant, Extra;
        };

        private int remaining = 0;
        private final long pubTime;
        private final HashMap<String, Tuple2<Integer, Long>> subscribers = new HashMap<>();

        public Delivery(long pubTime) {
            this.pubTime = pubTime;
        }

        public boolean addPending(String subscriber, Integer subGuid) {
            if (subscribers.containsKey(subscriber)) {
                return false;
            }
            subscribers.put(subscriber, new Tuple2<>(subGuid, null));
            remaining++;
            return true;
        }

        public DeliveryState addDelivery(String subscriber, Integer subGuid, Long receiveTime) {
            Tuple2<Integer, Long> tuple = subscribers.get(subscriber);
            if (tuple == null) {
                return DeliveryState.Extra;
            }
            if (tuple.getV2() != null) {
                return DeliveryState.Redundant;
            }
            tuple.setV2(receiveTime);
            remaining--;
            return remaining == 0 ? DeliveryState.Finish : DeliveryState.Deliver;
        }

        public Stream<String> getMissingSubscribers() {
            return subscribers.entrySet().stream()
                    .filter(e -> e.getValue().getV2() == null)
                    .map(e -> e.getKey());
        }

        public void printState(int pubId, PrintStream ps) {
            int count = 0;
            long total = 0, min = Long.MAX_VALUE, max = Long.MIN_VALUE;
            for (Tuple2<Integer, Long> value : subscribers.values()) {
                Long receiveTime = value.getV2();
                if (receiveTime != null) {
                    long latency = receiveTime - pubTime;
                    count++;
                    total += latency;
                    if (latency > max) {
                        max = latency;
                    }
                    if (latency < min) {
                        min = latency;
                    }
                }
            }
            if (count == 0) {
                // pubId pubTime count min max avg missing
                ps.printf("%d\t%d\t%d\t%d\t%d\t%f\t%d%n", pubId, pubTime, 0, 0, 0, 0d, remaining);
            } else {
                // pubId pubTime count min max avg missing
                ps.printf("%d\t%d\t%d\t%d\t%d\t%f\t%d%n", pubId, pubTime, count, min, max, total / (double) count, remaining);
            }
        }

        public double getAverageDeliverTime() {
            int count = 0;
            long totalTime = 0;
            for (Tuple2<Integer, Long> value : subscribers.values()) {
                Long receiveTime = value.getV2();
                if (receiveTime != null) {
                    count++;
                    totalTime += receiveTime - pubTime;
                }
            }
            return totalTime / count;
        }

    }

    @Test
    public void readResult() throws IOException {
        String resultFoldername = "./";
        String wikiFolderName = "/root/Wiki/";
        String outputFileName = "output_hld_3.txt";
        String deliveryFileName = "subset_deliveries_hld.txt";
        String resultFileName = "outputPerPacket_hld_3.txt";

        Iterator<String> outputIterator = Files.lines(Paths.get(resultFoldername, outputFileName)).iterator();
        Iterator<String> deliveryIterator = Files.lines(Paths.get(wikiFolderName, deliveryFileName)).iterator();

        // key: publication id
        // value: publication time, receivers
        // receivers.key: receiver
        // receivers.value: GUID, receive time
        HashMap<Integer, Delivery> deliveries = new HashMap<>();

        ReportObject ro = new ReportObject();
        ro.setKey("Delivery", 0);
        ro.setKey("Redundant", 1);
        ro.setKey("Extra", 2);
        ro.setKey("Finish", 3);
        ro.setKey("Missing", 4);
        ro.setKey("InTransit", () -> String.format("%,d", deliveries.size()));
        ro.setKey("Now", () -> String.format("%,d", Timeline.nowInUs()));

        try (PrintStream ps = new PrintStream(resultFileName)) {
            ps.println("PubID\tPubTime\tRcvCount\tMin\tMax\tAvg\tMissing");
            handleDeliveryLine(deliveryIterator, deliveries);
            handleOutputLine(outputIterator, deliveries, ro, ps);
            ro.beginReport();
            long finish = Timeline.run();
            System.out.printf("finish=%,d%n", finish);
            deliveries.forEach((pubId, delivery) -> {
                delivery.printState(pubId, ps);
                ro.incrementValue(4, delivery.remaining);
            });

            ro.endReport();
            ps.flush();

            // handle missing
        }
    }

    private void handleDeliveryLine(Iterator<String> deliveryIterator,
            HashMap<Integer, Delivery> deliveries) {
        if (!deliveryIterator.hasNext()) {
            return;
        }
        String line = deliveryIterator.next();
        // pubId, pubTime, subscriber, subGuid
        String[] parts = line.split("\t");
        assert parts.length == 4;
        Integer pubId = Integer.parseInt(parts[0]);
        Long pubTime = Long.parseLong(parts[1]);
        String subscriber = parts[2];
        Integer subGuid = Integer.parseInt(parts[3]);
        Timeline.addEvent(pubTime, this::deliveryHandler,
                deliveryIterator, deliveries, pubId, subscriber, subGuid);
    }

    @SuppressWarnings("unchecked")
    private void deliveryHandler(Object... params) {
        Iterator<String> deliveryIterator = (Iterator<String>) params[0];
        HashMap<Integer, Delivery> deliveries = (HashMap<Integer, Delivery>) params[1];
        Integer pubId = (Integer) params[2];
        String subscriber = (String) params[3];
        Integer subGuid = (Integer) params[4];

        Delivery delivery = deliveries.get(pubId);
        if (delivery == null) {
            deliveries.put(pubId, delivery = new Delivery(Timeline.nowInUs()));
        }
        boolean addSuccess = delivery.addPending(subscriber, subGuid);
        assert addSuccess;
        handleDeliveryLine(deliveryIterator, deliveries);
    }

    private void handleOutputLine(Iterator<String> outputIterator,
            HashMap<Integer, Delivery> deliveries,
            ReportObject ro, PrintStream ps) {
        if (!outputIterator.hasNext()) {
            return;
        }
        String line = outputIterator.next();
        // pubId, subscriber, receiveTime, subGuid
        String[] parts = line.split("\t");
        assert parts.length == 4;
        Integer pubId = Integer.parseInt(parts[0]);
        String subscriber = parts[1];
        Long receiveTime = Long.parseLong(parts[2]);
        Integer subGuid = Integer.parseInt(parts[3]);
        Timeline.addEvent(receiveTime, this::outputHandler,
                outputIterator, deliveries, ro, ps, pubId, subscriber, subGuid);
    }

    @SuppressWarnings("unchecked")
    private void outputHandler(Object... params) {
        Iterator<String> outputIterator = (Iterator<String>) params[0];
        HashMap<Integer, Delivery> deliveries = (HashMap<Integer, Delivery>) params[1];
        ReportObject ro = (ReportObject) params[2];
        PrintStream ps = (PrintStream) params[3];
        Integer pubId = (Integer) params[4];
        String subscriber = (String) params[5];
        Integer subGuid = (Integer) params[6];

        Delivery delivery = deliveries.get(pubId);
        if (delivery == null) {
            ro.incrementValue(2);
        } else {
            Delivery.DeliveryState state = delivery.addDelivery(subscriber, subGuid, Timeline.nowInUs());
            switch (state) {
                case Deliver:
                    ro.incrementValue(0);
                    break;
                case Extra:
                    ro.incrementValue(2);
                    break;
                case Finish:
                    ro.incrementValue(0);
                    ro.incrementValue(3);
                    delivery.printState(pubId, ps);
                    deliveries.remove(pubId);
                    break;
                case Redundant:
                    ro.incrementValue(1);
                    break;
                default:
                    assert false;
                    break;
            }
        }
        handleOutputLine(outputIterator, deliveries, ro, ps);
    }

}

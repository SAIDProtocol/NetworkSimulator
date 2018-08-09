/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.networksimulator.network.mf.graphpubsub;

import edu.rutgers.winlab.networksimulator.common.Data;
import edu.rutgers.winlab.networksimulator.common.PrioritizedQueue;
import edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets.SerialData;
import edu.rutgers.winlab.networksimulator.common.ReportObject;
import edu.rutgers.winlab.networksimulator.common.ReportingQueue;
import edu.rutgers.winlab.networksimulator.common.Timeline;
import edu.rutgers.winlab.networksimulator.common.Tuple1;
import edu.rutgers.winlab.networksimulator.common.Tuple2;
import edu.rutgers.winlab.networksimulator.common.Tuple3;
import edu.rutgers.winlab.networksimulator.common.UnlimitedQueue;
import edu.rutgers.winlab.networksimulator.network.Node;
import edu.rutgers.winlab.networksimulator.network.mf.MFGNRS;
import edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets.MFApplicationPacketPublication;
import edu.rutgers.winlab.networksimulator.network.mf.packets.GUID;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jiachen
 */
public class TraceReaderTest {

    public TraceReaderTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

//    @Test
    public void test1() throws IOException {
        String topologyPrefix = "/users/jiachen/Rocketfuel/1221/";
        String wikiPrefix = "/users/jiachen/Wiki/";

        String linksFile = topologyPrefix + "allLinks7.txt";
        String subscriptionFile = wikiPrefix + "subset_location_cat_subscriptions_modified.txt";
        String catRelationsipsFile = wikiPrefix + "subset_cat_subcats.txt";
        String gnrsRouterName = "Melbourne_+Australia734";
        String originalRPName = "Melbourne_+Australia3868";
        String pubRouterName = "Melbourne_+Australia3882";
        String newRPName = "Melbourne_+Australia751";
        String publicationsFile = wikiPrefix + "subset_publications.txt";
        String deliveriesFile = wikiPrefix + "subset_deliveries.txt";
        String partitionFile = "RANDOM.txt";
        double timeMultiplication = 1.0 / 1;   // 1000 / timeMultiplication pkts per second
        String resultFile = "output.txt";

        MFGNRS gnrs = new MFGNRS("GNRS", new UnlimitedQueue<>());
        HashMap<String, PrintStream> queuePses = new HashMap<>();

        Tuple1<Boolean> canMigrate = new Tuple1<>(true);
        Tuple1<MFPubSubRouter> originalRP = new Tuple1<>(null), newRP = new Tuple1<>(null);
        Set<GUID> migrateGUIDs = TraceReader.readPartitionFile(partitionFile).collect(Collectors.toSet());
        System.out.printf("toMigrate: %d%n", migrateGUIDs.size());

        BiConsumer<String, Integer> rpQueueConsumer = (q, l) -> {
            if (canMigrate.getV1() && l > 100) {
                System.out.println("Migrate!!!!!");
                canMigrate.setV1(false);
                migrateGUIDs.forEach(migrateGUID -> {
                    originalRP.getV1().moveRP(migrateGUID, newRP.getV1().getNa());
                });
            }
            try {
                PrintStream queuePs = queuePses.get(q);
                if (queuePs == null) {
                    queuePses.put(q, queuePs = new PrintStream("queue_" + q + ".txt"));
                }
                queuePs.printf("%d\t%d%n", Timeline.nowInUs(), l);
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        };
        BiConsumer<String, Integer> routerQueueConsumer = (q, l) -> {
            try {
                PrintStream queuePs = queuePses.get(q);
                if (queuePs == null) {
                    queuePses.put(q, queuePs = new PrintStream("queue_" + q + ".txt"));
                }
                queuePs.printf("%d\t%d%n", Timeline.nowInUs(), l);
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        };

        HashMap<String, MFPubSubRouter> routers = TraceReader.readNetworkTopology(
                gnrs, gnrsRouterName,
                linksFile, (name) -> {
//                    if (name.equals(originalRPName) || name.equals(newRPName)) {
//                        PrioritizedQueue<Tuple2<Node, Data>> inner = new UnlimitedQueue<>();
//                        ReportingQueue<Tuple2<Node, Data>, PrioritizedQueue<Tuple2<Node, Data>>> report = new ReportingQueue<>(name, inner, routerQueueConsumer);
//                        return report;
//                    }
                    return new UnlimitedQueue<>();
                }, name -> {
                    PrioritizedQueue<MFApplicationPacketPublication> inner = new UnlimitedQueue<>();
                    ReportingQueue<MFApplicationPacketPublication, PrioritizedQueue<MFApplicationPacketPublication>> report = new ReportingQueue<>(name, inner, rpQueueConsumer);
                    return report;
                }, () -> new UnlimitedQueue<>(),
                100 * Node.BW_IN_MBPS);

        routers.get("Melbourne_+Australia3882").setFib(routers.get("Melbourne_+Australia751").getNa(), new Tuple2<>(routers.get("Melbourne_+Australia734"), 2032L));

        originalRP.setV1(routers.get(originalRPName));
        newRP.setV1(routers.get(newRPName));
        MFPubSubRouter pubRouter = routers.get(pubRouterName);

        TraceReader.putAllGUIDsOnOneRouter(originalRP.getV1(), catRelationsipsFile);

        ReportObject ro = new ReportObject();
        ro.setKey("Receives", 1);
        ro.setKey("Redundant", 2);
        ro.setKey("Extra", 3);
        ro.setKey("Now", () -> String.format("%,d", Timeline.nowInUs()));

        HashMap<Tuple3<MFPubSubRouter, GUID, Integer>, Tuple3<String, Long, Long>> deliveries = new HashMap<>();
        BiConsumer<MFPubSubRouter, MFApplicationPacketPublication> consumer = (r, p) -> {
            SerialData sd = (SerialData) p.getPayload();
            Tuple3<MFPubSubRouter, GUID, Integer> key = new Tuple3<>(r, p.getDst(), sd.getId());
            Tuple3<String, Long, Long> val = deliveries.get(key);
            if (val == null) {
                ro.incrementValue(3);
            } else if (val.getV3() != Long.MIN_VALUE) {
                ro.incrementValue(2);
            } else {
                ro.incrementValue(1);
                val.setV3(Timeline.nowInUs());
            }
        };

        HashMap<String, Tuple2<GUID, MFPubSubRouter>> subscriptions = TraceReader.readSubscriptions(routers, subscriptionFile, consumer);
//        TraceReader.readDeliveries(deliveriesFile, subscriptions, timeMultiplication, deliveries);

        ro.beginReport();

        //TODO: modify here
        TraceReader.readPublications(publicationsFile, timeMultiplication, i -> {
//            if (canMigrate.getV1()) {
//                return originalRP.getV1();
//            } else if (migrateGUIDs.contains(i)) {
//                return newRP.getV1();
//            } else {
//                return originalRP.getV1();
//            }
            return pubRouter;
        });
        TraceReader.setReadStatistics(0, 300 * Timeline.SECOND, 100 * Timeline.MS, new MFPubSubRouter[]{originalRP.getV1(), newRP.getV1()});
        long finish = Timeline.run();
        System.out.printf("finish=%d%n", finish);
        ro.endReport();

        queuePses.values().forEach(ps -> {
            ps.flush();
            ps.close();
        });

        Files.write(Paths.get(resultFile),
                (Iterable<String>) deliveries.entrySet().stream()
                        .filter(e -> e.getValue().getV3() != Long.MIN_VALUE)
                        .sorted((e1, e2) -> e1.getKey().getV3().compareTo(e2.getKey().getV3()))
                        .map(e -> String.format("%d\t%s\t%d", e.getKey().getV3(), e.getValue().getV1(), e.getValue().getV3() - e.getValue().getV2()))::iterator
        );

        Tuple1<Long> networkTraffic = new Tuple1<>(0L);
        routers.values().forEach(r -> {
            r.forEachUnicastLink(l -> {
                networkTraffic.setV1(networkTraffic.getV1() + l.getBitsSent());
            });
        });
        System.out.printf("%,d%n", networkTraffic.getV1());

    }

//    @Test
    public void test2() throws IOException {
        String topologyPrefix = "/users/jiachen/Rocketfuel/1221/";
        String wikiPrefix = "/users/jiachen/Wiki/";

        String linksFile = topologyPrefix + "allLinks7.txt";
        String subscriptionFile = wikiPrefix + "subset_location_cat_subscriptions_modified.txt";
        String catRelationsipsFile = wikiPrefix + "subset_cat_name_subcats.txt";
        String gnrsRouterName = "Melbourne_+Australia734";
        String originalRPName = "Melbourne_+Australia3868";
        String pubRouterName = "Melbourne_+Australia3882";
        String newRPName = "Melbourne_+Australia751";
        String publicationsFile = wikiPrefix + "subset_publications.txt";
        String deliveriesFile = wikiPrefix + "subset_deliveries.txt";
        String nameMappingFile = wikiPrefix + "subset_cat_namemappings.txt";
        String partitionFile = "METIS.txt";
        double timeMultiplication = 1.0 / 1;   // 1000 / timeMultiplication pkts per second
        String resultFile = "output.txt";

        MFGNRS gnrs = new MFGNRS("GNRS", new UnlimitedQueue<>());
        HashMap<String, PrintStream> queuePses = new HashMap<>();
        HashMap<GUID, GUID[]> catToHierarcalMapping = new HashMap<>();
        HashMap<GUID, GUID> hierarchicalToCatMapping = new HashMap<>();

        TraceReader.readNameMapping(nameMappingFile, catToHierarcalMapping, hierarchicalToCatMapping);
        System.out.printf("%d\t%d%n", catToHierarcalMapping.size(), hierarchicalToCatMapping.size());

        Tuple1<Boolean> canMigrate = new Tuple1<>(false);
        Tuple1<MFPubSubRouter> originalRP = new Tuple1<>(null), newRP = new Tuple1<>(null);
        Set<GUID> migrateGUIDs = TraceReader.readPartitionFile(partitionFile).collect(Collectors.toSet());
        System.out.printf("toMigrate: %d%n", migrateGUIDs.size());

        BiConsumer<String, Integer> rpQueueConsumer = (q, l) -> {
//            if (canMigrate.getV1() && l > 100) {
//                System.out.println("Migrate!!!!!");
//                canMigrate.setV1(false);
//                migrateGUIDs.forEach(migrateGUID -> {
//                    originalRP.getV1().moveRP(migrateGUID, newRP.getV1().getNa());
//                });
//            }
            try {
                PrintStream queuePs = queuePses.get(q);
                if (queuePs == null) {
                    queuePses.put(q, queuePs = new PrintStream("queue_" + q + ".txt"));
                }
                queuePs.printf("%d\t%d%n", Timeline.nowInUs(), l);
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        };
//        BiConsumer<String, Integer> routerQueueConsumer = (q, l) -> {
//            try {
//                PrintStream queuePs = queuePses.get(q);
//                if (queuePs == null) {
//                    queuePses.put(q, queuePs = new PrintStream("queue_" + q + ".txt"));
//                }
//                queuePs.printf("%d\t%d%n", Timeline.nowInUs(), l);
//            } catch (IOException e) {
//                e.printStackTrace(System.err);
//            }
//        };
//
        HashMap<String, MFPubSubRouter> routers = TraceReader.readNetworkTopology(
                gnrs, gnrsRouterName,
                linksFile, (name) -> {
//                    if (name.equals(originalRPName) || name.equals(newRPName)) {
//                        PrioritizedQueue<Tuple2<Node, Data>> inner = new UnlimitedQueue<>();
//                        ReportingQueue<Tuple2<Node, Data>, PrioritizedQueue<Tuple2<Node, Data>>> report = new ReportingQueue<>(name, inner, routerQueueConsumer);
//                        return report;
//                    }
                    return new UnlimitedQueue<>();
                }, name -> {
                    PrioritizedQueue<MFApplicationPacketPublication> inner = new UnlimitedQueue<>();
                    ReportingQueue<MFApplicationPacketPublication, PrioritizedQueue<MFApplicationPacketPublication>> report = new ReportingQueue<>(name, inner, rpQueueConsumer);
                    return report;
                }, () -> new UnlimitedQueue<>(),
                100 * Node.BW_IN_MBPS);

        routers.get("Melbourne_+Australia3882").setFib(routers.get("Melbourne_+Australia751").getNa(), new Tuple2<>(routers.get("Melbourne_+Australia734"), 2032L));

        originalRP.setV1(routers.get(originalRPName));
        newRP.setV1(routers.get(newRPName));
        MFPubSubRouter pubRouter = routers.get(pubRouterName);

        TraceReader.putAllGUIDsOnOneRouter(originalRP.getV1(), catRelationsipsFile);
//        gnrs.forEachStorage((g, t) -> {
//            System.out.println(g.getRepresentation());
//        });
//        System.out.printf("Total: %d%n", gnrs.getStorageStream().count());

        ReportObject ro = new ReportObject();
        ro.setKey("Receives", 1);
        ro.setKey("Redundant", 2);
        ro.setKey("Extra", 3);
        ro.setKey("Now", () -> String.format("%,d", Timeline.nowInUs()));
//
        HashMap<Tuple3<MFPubSubRouter, GUID, Integer>, Tuple3<String, Long, Long>> deliveries = new HashMap<>();
        BiConsumer<MFPubSubRouter, MFApplicationPacketPublication> consumer = (r, p) -> {
            SerialData sd = (SerialData) p.getPayload();
            GUID origGUID = hierarchicalToCatMapping.get(p.getDst());
            Tuple3<MFPubSubRouter, GUID, Integer> key = new Tuple3<>(r, origGUID, sd.getId());
            Tuple3<String, Long, Long> val = deliveries.get(key);
            if (val == null) {
                ro.incrementValue(3);
            } else if (val.getV3() != Long.MIN_VALUE) {
                ro.incrementValue(2);
            } else {
                ro.incrementValue(1);
                val.setV3(Timeline.nowInUs());
            }
        };
//
        HashMap<String, Tuple2<GUID, MFPubSubRouter>> subscriptions = TraceReader.readSubscriptionsForHierarchical(routers, catToHierarcalMapping, subscriptionFile, consumer, 0);
        TraceReader.readDeliveries(deliveriesFile, subscriptions, timeMultiplication, deliveries);
//
        ro.beginReport();
//
//        //TODO: modify here
        TraceReader.readPublicationsForHierarchical(publicationsFile, catToHierarcalMapping, timeMultiplication, i -> {
//            if (canMigrate.getV1()) {
//                return originalRP.getV1();
//            } else if (migrateGUIDs.contains(i)) {
//                return newRP.getV1();
//            } else {
//                return originalRP.getV1();
//            }
            return pubRouter;
        });
        TraceReader.setReadStatistics(0, 300 * Timeline.SECOND, 100 * Timeline.MS, new MFPubSubRouter[]{originalRP.getV1(), newRP.getV1()});
        long finish = Timeline.run();
        System.out.printf("finish=%d%n", finish);
        ro.endReport();

        queuePses.values().forEach(ps -> {
            ps.flush();
            ps.close();
        });

        Files.write(Paths.get(resultFile),
                (Iterable<String>) deliveries.entrySet().stream()
                        .filter(e -> e.getValue().getV3() != Long.MIN_VALUE)
                        .sorted((e1, e2) -> e1.getKey().getV3().compareTo(e2.getKey().getV3()))
                        .map(e -> String.format("%d\t%s\t%d", e.getKey().getV3(), e.getValue().getV1(), e.getValue().getV3() - e.getValue().getV2()))::iterator
        );

        Tuple1<Long> networkTraffic = new Tuple1<>(0L);
        routers.values().forEach(r -> {
            r.forEachUnicastLink(l -> {
                networkTraffic.setV1(networkTraffic.getV1() + l.getBitsSent());
            });
        });
        System.out.printf("%,d%n", networkTraffic.getV1());
    }

}

package edu.rutgers.winlab.networksimulator.network.mf.graphpubsub;

import edu.rutgers.winlab.networksimulator.common.Data;
import edu.rutgers.winlab.networksimulator.common.PrioritizedQueue;
import edu.rutgers.winlab.networksimulator.common.Timeline;
import edu.rutgers.winlab.networksimulator.common.Tuple2;
import edu.rutgers.winlab.networksimulator.common.Tuple3;
import edu.rutgers.winlab.networksimulator.common.Tuple4;
import edu.rutgers.winlab.networksimulator.network.Node;
import edu.rutgers.winlab.networksimulator.network.mf.MFGNRS;
import edu.rutgers.winlab.networksimulator.network.mf.MFRouter;
import edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets.MFApplicationPacketPublication;
import edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets.SerialData;
import edu.rutgers.winlab.networksimulator.network.mf.packets.GUID;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 *
 * @author Jiachen Chen
 */
public class TraceReader {

    // linksFile: n1 n2 latency
    public static HashMap<String, MFPubSubRouter> readNetworkTopology(
            MFGNRS gnrs,
            String gnrsRouterName,
            String linksFile,
            Function<String, PrioritizedQueue<Tuple2<Node, Data>>> incomingQueueGenerator,
            Function<String, PrioritizedQueue<MFApplicationPacketPublication>> rpQueueGenerator,
            Supplier<PrioritizedQueue<Data>> linkQueueGenerator,
            int bwInBitsPerMs)
            throws IOException {
        HashMap<String, MFPubSubRouter> routers = new HashMap<>();
        Files.lines(Paths.get(linksFile)).forEach(l -> {
            String[] parts = l.split("\t");
            assert parts.length == 3;
            String n1Name = parts[0], n2Name = parts[1];
            long latency = Integer.parseInt(parts[2]) * Timeline.MS;
            MFPubSubRouter n1 = routers.get(n1Name);
            if (n1 == null) {
                routers.put(n1Name, n1 = new MFPubSubRouter(n1Name, incomingQueueGenerator.apply(n1Name), gnrs.getNa(), rpQueueGenerator.apply(n1Name + "_RP")));
            }
            MFPubSubRouter n2 = routers.get(n2Name);
            if (n2 == null) {
                routers.put(n2Name, n2 = new MFPubSubRouter(n2Name, incomingQueueGenerator.apply(n2Name), gnrs.getNa(), rpQueueGenerator.apply(n2Name + "_RP")));
            }
            Node.linkNodes(n1, n2, bwInBitsPerMs, latency, linkQueueGenerator.get(), linkQueueGenerator.get());
        });

        MFPubSubRouter gnrsRouter = routers.get(gnrsRouterName);
        Node.linkNodes(gnrsRouter, gnrs, bwInBitsPerMs, 1 * Timeline.MS, linkQueueGenerator.get(), linkQueueGenerator.get());

        Timeline.addEvent(0, ps -> ((MFRouter) ps[0]).announceNA(), gnrs);
        Timeline.run();

        routers.forEach((n, r) -> {
            Timeline.addEvent(0, ps -> ((MFRouter) ps[0]).announceNA(), r);
            Timeline.run();
        });
//
//        HashMap<Node, Long> nodeLatencies = new HashMap<>();
//
//        routers.forEach((n, r) -> {
//            r.forEachFib((na, t) -> {
//                nodeLatencies.merge(na.getNode(), t.getV2(), Long::sum);
//            });
//        });
//        nodeLatencies.forEach((n, l) -> System.out.printf("%s\t%,d%n", n.getName(), l));
//
        return routers;
    }

    // catSubCatsFile: catId catName parentCatId parentCatName
    public static void putAllGUIDsOnOneRouter(MFPubSubRouter router, String cateSubCatsFile) throws IOException {
        HashSet<GUID> guids = new HashSet<>();
        ArrayList<Tuple2<GUID, GUID>> relationships = new ArrayList<>();
        Files.lines(Paths.get(cateSubCatsFile)).forEach(l -> {
            String[] parts = l.split("\t");
            assert parts.length == 4;
            GUID child = new GUID(Integer.parseInt(parts[0]));
            guids.add(child);
            GUID parent = new GUID(Integer.parseInt(parts[2]));
            guids.add(parent);
            relationships.add(new Tuple2<>(child, parent));
        });

        Timeline.addEvent(0, ps -> {
            HashSet<GUID> p0 = (HashSet<GUID>) ps[0];
            ArrayList<Tuple2<GUID, GUID>> p1 = (ArrayList<Tuple2<GUID, GUID>>) ps[1];
            MFPubSubRouter p2 = (MFPubSubRouter) ps[2];
            p0.forEach(g -> p2.registerRP(g));
            // traffic should flow from child category to parent category here
            p1.forEach(t -> p2.addGraphRelationship(t.getV1(), t.getV2()));
        }, guids, relationships, router);
        long finish = Timeline.run();
        System.out.printf("putAllGUIDsOnOneRouter: %,d%n", finish);
    }

    // subscritionFile: routerId subId subCatId subCatName
    // return: key:subId value: subCatId routerId
    public static HashMap<String, Tuple2<GUID, MFPubSubRouter>> readSubscriptions(
            HashMap<String, MFPubSubRouter> routers,
            String locationSubscriptionFile,
            BiConsumer<? super MFPubSubRouter, ? super MFApplicationPacketPublication> publicationHandler)
            throws IOException {
        HashMap<String, Tuple2<GUID, MFPubSubRouter>> subscriptions = new HashMap<>();

        Files.lines(Paths.get(locationSubscriptionFile)).forEach(l -> {
            String[] parts = l.split("\t");
            assert parts.length == 4;
            String routerName = parts[0];
            String subscriberName = parts[1];
            GUID guid = new GUID(Integer.parseInt(parts[2]));
            MFPubSubRouter router = routers.get(routerName);
            subscriptions.put(subscriberName, new Tuple2<>(guid, router));
        });

        Timeline.addEvent(0, ps -> {
            Collection<Tuple2<GUID, MFPubSubRouter>> p0 = (Collection<Tuple2<GUID, MFPubSubRouter>>) ps[0];
            BiConsumer<? super MFPubSubRouter, ? super MFApplicationPacketPublication> p1 = (BiConsumer<? super MFPubSubRouter, ? super MFApplicationPacketPublication>) ps[1];
            p0.forEach(t -> {
                t.getV2().subscribe(t.getV1(), p1);
            });
        }, subscriptions.values(), publicationHandler);
        long finish = Timeline.run();
        System.out.printf("Finish=%,d%n", finish);
        return subscriptions;
    }

    // subscritionFile: routerId subId subCatId subCatName
    // return: key:subId value: subCatId routerId
    public static HashMap<String, Tuple2<GUID, MFPubSubRouter>> readSubscriptionsForHierarchical(
            HashMap<String, MFPubSubRouter> routers,
            HashMap<GUID, GUID[]> catToHierarchicalMapping,
            String locationSubscriptionFile,
            BiConsumer<? super MFPubSubRouter, ? super MFApplicationPacketPublication> publicationHandler,
            long randomSeed)
            throws IOException {
        HashMap<String, Tuple2<GUID, MFPubSubRouter>> subscriptions = new HashMap<>();
        Random rand = new Random();

        Files.lines(Paths.get(locationSubscriptionFile)).forEach(l -> {
            String[] parts = l.split("\t");
            assert parts.length == 4;
            String routerName = parts[0];
            String subscriberName = parts[1];
            GUID guid = new GUID(Integer.parseInt(parts[2]));
            MFPubSubRouter router = routers.get(routerName);
            subscriptions.put(subscriberName, new Tuple2<>(guid, router));
        });

        Timeline.addEvent(0, ps -> {
            Collection<Tuple2<GUID, MFPubSubRouter>> p0 = (Collection<Tuple2<GUID, MFPubSubRouter>>) ps[0];
            BiConsumer<? super MFPubSubRouter, ? super MFApplicationPacketPublication> p1 = (BiConsumer<? super MFPubSubRouter, ? super MFApplicationPacketPublication>) ps[1];
            p0.forEach(t -> {
                GUID[] targets = catToHierarchicalMapping.get(t.getV1());
                GUID toSubscribe = targets[rand.nextInt(targets.length)];
                t.getV2().subscribe(toSubscribe, p1);
            });
        }, subscriptions.values(), publicationHandler);
        long finish = Timeline.run();
        System.out.printf("Finish=%,d%n", finish);
        return subscriptions;
    }

    // deliveriesFile: pubId pubTime subId subCatId
    // deliveries: key: router receiveGuid pubId,   value: sub_name send_time receive_time
    public static void readDeliveries(
            String deliveriesFile,
            HashMap<String, Tuple2<GUID, MFPubSubRouter>> subscriptions,
            double timeMultiplication,
            HashMap<Tuple3<MFPubSubRouter, GUID, Integer>, Tuple3<String, Long, Long>> deliveries)
            throws IOException {
        Files.lines(Paths.get(deliveriesFile)).forEach(l -> {
            String[] parts = l.split("\t");
            assert parts.length == 4;
            int pubId = Integer.parseInt(parts[0]);
            long pubTime = (long) (Long.parseLong(parts[1]) * timeMultiplication);
            String subName = parts[2];
            Tuple2<GUID, MFPubSubRouter> sub = subscriptions.get(subName);
            assert sub != null;
            GUID guid = new GUID(Integer.parseInt(parts[3]));
            if (!guid.equals(sub.getV1())) {
                System.err.println(l);
                System.err.printf("GUID:%d, sub.getV1:%d%n", guid.getRepresentation(), sub.getV1().getRepresentation());
                System.err.flush();
            }
            assert guid.equals(sub.getV1());
            Tuple3<String, Long, Long> orig = deliveries.put(new Tuple3<>(sub.getV2(), sub.getV1(), pubId), new Tuple3<>(subName, pubTime, Long.MIN_VALUE));
            assert orig == null;
        });
    }

    // publicationFile: pubId pubTime pubCatId
    public static void readPublications(String publicationFile, double timeMultiplication,
            Function<GUID, MFPubSubRouter> pubGetter) throws IOException {
        Iterator<String> it = Files.lines(Paths.get(publicationFile)).iterator();
        if (!it.hasNext()) {
            return;
        }
        Tuple3<Integer, Long, GUID> pub = readPublicationLine(it.next(), timeMultiplication);
        Timeline.addEvent(pub.getV2(), TraceReader::timelineRunner, pub, it, timeMultiplication, pubGetter);
    }

    // publicationFile: pubId pubTime pubCatId
    private static Tuple3<Integer, Long, GUID> readPublicationLine(String line, double timeMultiplication) {
        String[] parts = line.split("\t");
        assert parts.length == 4;
        int pubId = Integer.parseInt(parts[0]);
        long pubTime = (long) (Long.parseLong(parts[1]) * timeMultiplication);
        GUID pubGUID = new GUID(Integer.parseInt(parts[2]));
        return new Tuple3<>(pubId, pubTime, pubGUID);
    }

    private static void timelineRunner(Object... params) {
        Tuple3<Integer, Long, GUID> pub = (Tuple3<Integer, Long, GUID>) params[0];
        Function<GUID, MFPubSubRouter> pubGetter = (Function<GUID, MFPubSubRouter>) params[3];
        MFPubSubRouter router = pubGetter.apply(pub.getV3());
        router.publish(null, pub.getV3(), new SerialData(pub.getV1(), 1000 * Data.BYTE));
//        System.out.printf("%d\t%d\t%d\t%s%n", pub.getV1(), Timeline.nowInUs(), pub.getV3().getRepresentation(), router.getName());

        Iterator<String> it = (Iterator<String>) params[1];
        double timeMultiplication = (Double) params[2];
        if (!it.hasNext()) {
            return;
        }
        pub = readPublicationLine(it.next(), timeMultiplication);
        Timeline.addEvent(pub.getV2(), TraceReader::timelineRunner, pub, it, timeMultiplication, pubGetter);
    }

    // publicationFile: pubId pubTime pubCatId
    public static void readPublicationsForHierarchical(String publicationFile,
            HashMap<GUID, GUID[]> catToHierarchicalMapping,
            double timeMultiplication,
            Function<GUID, MFPubSubRouter> pubGetter) throws IOException {
        Iterator<String> it = Files.lines(Paths.get(publicationFile)).iterator();
        if (!it.hasNext()) {
            return;
        }
        Tuple3<Integer, Long, GUID> pub = readPublicationLine(it.next(), timeMultiplication);
        Timeline.addEvent(pub.getV2(), TraceReader::timelineRunnerForHierarchical, pub, it, timeMultiplication, pubGetter, catToHierarchicalMapping);
    }

    private static void timelineRunnerForHierarchical(Object... params) {
        Tuple3<Integer, Long, GUID> pub = (Tuple3<Integer, Long, GUID>) params[0];
        Function<GUID, MFPubSubRouter> pubGetter = (Function<GUID, MFPubSubRouter>) params[3];
        HashMap<GUID, GUID[]> catToHierarchicalMapping = (HashMap< GUID, GUID[]>) params[4];
        MFPubSubRouter router = pubGetter.apply(pub.getV3());
        GUID[] toPublish = catToHierarchicalMapping.get(pub.getV3());
        for (GUID guid : toPublish) {
            router.publish(null, guid, new SerialData(pub.getV1(), 1000 * Data.BYTE));
        }

//        System.out.printf("%d\t%d\t%d\t%s%n", pub.getV1(), Timeline.nowInUs(), pub.getV3().getRepresentation(), router.getName());
        Iterator<String> it = (Iterator<String>) params[1];
        double timeMultiplication = (Double) params[2];
        if (!it.hasNext()) {
            return;
        }
        pub = readPublicationLine(it.next(), timeMultiplication);
        Timeline.addEvent(pub.getV2(), TraceReader::timelineRunnerForHierarchical, pub, it, timeMultiplication, pubGetter, catToHierarchicalMapping);
    }

    // partitionFile: catId partition (0/1)
    public static Stream<GUID> readPartitionFile(String partitionFile) throws IOException {
        HashSet<String> zeros = new HashSet<>(), ones = new HashSet<>();
        Files.lines(Paths.get(partitionFile)).forEach(l -> {
            String[] parts = l.split("\t");
            assert parts.length == 2;
            switch (parts[1]) {
                case "0":
                    zeros.add(parts[0]);
                    break;
                case "1":
                    ones.add(parts[0]);
                    break;
                default:
                    assert false;
            }
        });
        HashSet<String> min = zeros.size() < ones.size() ? zeros : ones;
        return min.stream().map(s -> new GUID(Integer.parseInt(s)));
    }

    public static void setReadStatistics(long start, long end, long period, MFPubSubRouter[] routers) {
        HashMap<MFPubSubRouter, Tuple4<PrintStream, Integer, Integer, Integer>> routerPs = new HashMap<>();
        for (MFPubSubRouter router : routers) {
            try {
                PrintStream ps = new PrintStream(router.getName() + "_stat.txt");
                ps.println("Time\tIncomingPublication\toutgoingUnicast\toutgoingMulticast");
                routerPs.put(router, new Tuple4<>(ps, router.getIncomingPublication(), router.getOutgoingUnicast(), router.getOutgoingMulticast()));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(TraceReader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        Timeline.addEvent(start, TraceReader::statisticsRunner, period, end, routerPs);
    }

    private static void statisticsRunner(Object... params) {
        long period = (Long) params[0];
        long end = (Long) params[1];
        long now = Timeline.nowInUs();
        HashMap<MFPubSubRouter, Tuple4<PrintStream, Integer, Integer, Integer>> routerPs = (HashMap<MFPubSubRouter, Tuple4<PrintStream, Integer, Integer, Integer>>) params[2];
        routerPs.forEach((r, t) -> {
            int incomingPublication = r.getIncomingPublication(),
                    outgoingUnicast = r.getOutgoingUnicast(),
                    outgoingMulticast = r.getOutgoingMulticast();
            int origIncomingPublication = t.getV2(),
                    origOutgoingUnicast = t.getV3(),
                    origOutgoingMulticast = t.getV4();
            t.getV1().printf("%d\t%d\t%d\t%d%n", now, incomingPublication - origIncomingPublication, outgoingUnicast - origOutgoingUnicast, outgoingMulticast - origOutgoingMulticast);
            t.setV2(incomingPublication);
            t.setV3(outgoingUnicast);
            t.setV4(outgoingMulticast);
        });
        if (now + period > end) {
            routerPs.forEach((r, t) -> {
                t.getV1().flush();
                t.getV1().close();
            });
        } else {
            Timeline.addEvent(now + period, TraceReader::statisticsRunner, params);
        }

    }

    public static void readNameMapping(String mappingFile, HashMap<GUID, GUID[]> catToHierarchicalMapping, HashMap<GUID, GUID> hierarchicalToCatMapping) throws IOException {
        Files.lines(Paths.get(mappingFile)).forEach(l -> {
            String[] parts = l.split("\t");
            assert parts.length > 3;
            GUID catGuid = new GUID(Integer.parseInt(parts[0]));
            int nameCount = Integer.parseInt(parts[2]);
            assert parts.length == nameCount * 2 + 3;
            GUID[] hierarchicalNames = new GUID[nameCount];
            for (int i = 0; i < nameCount; i++) {
                hierarchicalNames[i] = new GUID(Integer.parseInt(parts[3 + 2 * i]));
            }
            GUID[] tmp = catToHierarchicalMapping.put(catGuid, hierarchicalNames);
            assert tmp == null;
            for (GUID hierarchicalName : hierarchicalNames) {
                GUID tmp2 = hierarchicalToCatMapping.put(hierarchicalName, catGuid);
                assert tmp2 == null;
            }
        });
    }
}

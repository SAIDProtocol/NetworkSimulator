package edu.rutgers.winlab.networksimulator.network.gpser;

import edu.rutgers.winlab.networksimulator.common.Tuple2;
import edu.rutgers.winlab.networksimulator.network.gpser.packets.GpserPacketSubscription;
import edu.rutgers.winlab.networksimulator.network.gpser.packets.Name;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Stream;

/**
 *
 * @author Jiachen Chen
 * @param <T> interface type
 */
public class SubscriptionTable<T> {

    private final HashMap<Name, HashSet<T>> namesOutgoingInterfaces = new HashMap<>();
    private final HashMap<Name, HashSet<Name>> coveredBy = new HashMap<>();

    public Stream<Tuple2<Name, Stream<T>>> getOutgoingInterfaces() {
        return namesOutgoingInterfaces.entrySet()
                .stream().map(e -> new Tuple2<>(e.getKey(), e.getValue().stream()));
    }

    public Stream<Tuple2<Name, Stream<Name>>> getCoveredBy() {
        return coveredBy.entrySet()
                .stream().map(e -> new Tuple2<>(e.getKey(), e.getValue().stream()));
    }

    /**
     * Process the subscriptions and unsubscriptions in the packet, assuming
     * that the packet is from interface "from".
     *
     * @param subscription
     * @param from
     * @return The subscriptions and unsubscriptions to be sent upstream.
     */
    public GpserPacketSubscription handleSubscription(GpserPacketSubscription subscription, T from) {
        // handle subscriptions first
        GpserPacketSubscription ret = new GpserPacketSubscription();
        subscription.getSubscriptions().filter(e -> e.getValue())
                .map(e -> e.getKey())
                .forEach(name -> {
                    HashSet<T> nameOutgoingInterfaces = namesOutgoingInterfaces.get(name);
                    if (nameOutgoingInterfaces != null) { // already has downstream subscribed to this name, add interface and do nothing
                        nameOutgoingInterfaces.add(from); // doesn't matter if "from" is already there. add anyways and do nothing anyways
                    } else {
                        // N->DU, C->D
                        nameOutgoingInterfaces = new HashSet<>();
                        nameOutgoingInterfaces.add(from);
                        namesOutgoingInterfaces.put(name, nameOutgoingInterfaces);
                        // N->DU
                        if (!coveredBy.containsKey(name)) {
                            ret.subscribe(name);
                        }
                        name.getAncestors().forEach(n -> {
                            HashSet<Name> covered = coveredBy.get(n);
                            // N->C, DU->D
                            if (covered == null) {
                                // DU->D
                                if (namesOutgoingInterfaces.containsKey(n)) {
                                    ret.unsubscribe(n);
                                }
                                coveredBy.put(n, covered = new HashSet<>());
                            }
                            covered.add(name);
                        });
                    }
                });
        // handle unsubscriptions
        subscription.getSubscriptions().filter(e -> !e.getValue())
                .map(e -> e.getKey())
                .forEach(name -> {
                    HashSet<T> nameOutgoingInterfaces = namesOutgoingInterfaces.get(name);
                    // make sure that we have subscribed to this name from this interface before, and removed all the interfaces
                    if (nameOutgoingInterfaces != null && nameOutgoingInterfaces.remove(from) && nameOutgoingInterfaces.isEmpty()) {
                        // DU->N, D->C
                        namesOutgoingInterfaces.remove(name);
                        // DU->N
                        if (!coveredBy.containsKey(name)) {
                            ret.unsubscribe(name);
                        }
                        name.getAncestors().forEach(n -> {
                            HashSet<Name> covered = coveredBy.get(n);
                            assert covered != null;
                            boolean removed = covered.remove(name);
                            assert removed;
                            // C->N, D->DU
                            if (covered.isEmpty()) {
                                // D->DU
                                if (namesOutgoingInterfaces.containsKey(n)) {
                                    ret.subscribe(n);
                                }
                                coveredBy.remove(n);
                            }
                        });
                    }
                });

        return ret;
    }

    /**
     * Gets the outgoing interfaces for a name (and its descendents).
     *
     * @param n
     * @return
     */
    public Stream<T> getNameInterfaces(Name n) {
        HashSet<T> ret = new HashSet<>();
        {
            HashSet<T> curr = namesOutgoingInterfaces.get(n);
            if (curr != null) {
                ret.addAll(curr);
            }
        }
        HashSet<Name> covered = coveredBy.get(n);
        if (covered != null) {
            covered.forEach(c -> {
                HashSet<T> curr = namesOutgoingInterfaces.get(c);
                if (curr != null) {
                    ret.addAll(curr);
                }
            });
        }
        return ret.stream();
    }
}

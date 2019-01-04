package edu.rutgers.winlab.networksimulator.network.gpser.packets;

import edu.rutgers.winlab.networksimulator.common.Data;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 *
 * @author Jiachen Chen
 */
public class GpserPacketSubscription implements Data {

    private static final Logger LOG = Logger.getLogger(GpserPacketSubscription.class.getName());

    private final HashMap<Name, Boolean> subscriptions = new HashMap<>();

    public void subscribe(Name name) {
        Boolean b = subscriptions.get(name);
        if (b == null) {
            subscriptions.put(name, true);
        } else if (!b) { // subscription after unsubsccription, cancel out
//            LOG.log(Level.INFO, "subscription after unsubscription on name {0}, cancel out", name);
            subscriptions.remove(name);
        } else { // subscription after subscription, do nothing
            LOG.log(Level.WARNING, "subscription after subscription on name {0}", name);
        }
    }

    public void unsubscribe(Name name) {
        Boolean b = subscriptions.get(name);
        if (b == null) {
            subscriptions.put(name, false);
        } else if (b) { // unsubscription after subsccription, cancel out
//            LOG.log(Level.INFO, "unsubscription after subscription on name {0}, cancel out", name);
            subscriptions.remove(name);
        } else {// unsubscription after unsubscription, do nothing
            LOG.log(Level.WARNING, "unsubscription after unsubscription on name {0}", name);
        }
    }

    public Stream<Entry<Name, Boolean>> getSubscriptions() {
        return subscriptions.entrySet().stream();
    }

    @Override
    public int getSizeInBits() {
        return subscriptions.size() * Name.NAME_SIZE // names
                + (subscriptions.size() + BYTE - 1) / BYTE; // subscriptions, 1 bit for each subscription
    }

}

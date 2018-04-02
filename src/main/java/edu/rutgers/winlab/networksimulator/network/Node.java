package edu.rutgers.winlab.networksimulator.network;

import edu.rutgers.winlab.networksimulator.common.Data;
import edu.rutgers.winlab.networksimulator.common.PrioritizedQueue;
import edu.rutgers.winlab.networksimulator.common.QueuePoller;
import edu.rutgers.winlab.networksimulator.common.Timeline;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Represents a physical node in the network
 *
 * @author Jiachen Chen
 */
public abstract class Node {

    public static final long BW_IN_KBPS = Data.K_BIT / Timeline.MS_IN_SECOND;
    public static final long BW_IN_MBPS = Data.M_BIT / Timeline.MS_IN_SECOND;

    private final String name;
    private final HashMap<Node, Link> links = new HashMap<>();
    private final ArrayList<Link> abortedLinks = new ArrayList<>();

    public static void linkNodes(Node n1, Node n2, int bwInBitsPerMs, long delayInMs, PrioritizedQueue queueN1N2, PrioritizedQueue queueN2N1) {
        n1.link(n2, bwInBitsPerMs, delayInMs, queueN1N2);
        n2.link(n1, bwInBitsPerMs, delayInMs, queueN2N1);
    }

    public static void disconnectNodes(Node n1, Node n2) {
        n1.disconnect(n2);
        n2.disconnect(n1);
    }

    public Node(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Stream<Link> linkStream() {
        return Stream.concat(links.values().stream(), abortedLinks.stream());
    }

    public Stream<Link> connectedLinkStream() {
        return links.values().stream();
    }

    public Stream<Link> abortedLinkStream() {
        return abortedLinks.stream();
    }

    public void forEachLink(Consumer<? super Link> consumer) {
        forEachConnectedLink(consumer);
        forEachAbortedLink(consumer);
    }

    public void forEachConnectedLink(Consumer<? super Link> consumer) {
        links.values().forEach(consumer);
    }

    public void forEachAbortedLink(Consumer<? super Link> consumer) {
        abortedLinks.forEach(consumer);
    }

    public void clearAbortedLinks() {
        abortedLinks.clear();
    }

    private void link(Node another, int bwInBitsPerMs, long delayInMs, PrioritizedQueue queue) {
        Link l = links.put(another, new Link(another, bwInBitsPerMs, delayInMs, queue));
        if (l != null) {
            throw new IllegalArgumentException(String.format("%s is already linked to %s.", name, another.name));
        }
    }

    private void disconnect(Node another) {
        Link l = links.remove(another);
        if (l != null) {
            l.abort();
            abortedLinks.add(l);
        }
    }

    protected abstract void handleData(Node source, Data d);

    protected void sendData(Node destination, Data d, boolean prioritized) {
        Link l = links.get(destination);
        if (l == null) {
            throw new IllegalArgumentException(String.format("%s is not linked with %s", name, destination.name));
        }
        l.send(d, prioritized);
    }

    public class Link {

        private final Node destination;
        private final int bwBitsPerMS;
        private final long delayInUS;
        private final QueuePoller queuePoller;
        private long bitsSent = 0, bitsDiscarded = 0;
        private boolean busy = false, connected = true;

        public Link(Node destination, int bwBitsPerMS, long delayInMS, PrioritizedQueue queue) {
            this.destination = destination;
            this.bwBitsPerMS = bwBitsPerMS;
            this.delayInUS = delayInMS * Timeline.US_IN_MS;
            queuePoller = new QueuePoller(this::handleData, queue);
        }

        public Node getDestination() {
            return destination;
        }

        public Node getSource() {
            return Node.this;
        }

        public int getBwBitsPerMS() {
            return bwBitsPerMS;
        }

        public long getDelayInUS() {
            return delayInUS;
        }

        public long getBitsSent() {
            return bitsSent;
        }

        public long getBitsDiscarded() {
            return bitsDiscarded;
        }

        public boolean isBusy() {
            return busy;
        }

        public boolean isConnected() {
            return connected;
        }

        public void send(Data data, boolean prioritized) {
            bitsDiscarded += queuePoller.enQueue(data, prioritized);
        }

        public void abort() {
            connected = false;
            bitsDiscarded += queuePoller.clear();
        }

        private long handleData(Data d) {
            int sizeInBits = d.getSizeInBits();
            long transmitTimeInUs = sizeInBits * Timeline.US_IN_MS / bwBitsPerMS;
            long now = Timeline.nowInUs();
            long arrivalTime = now + transmitTimeInUs + delayInUS;
            Timeline.addEvent(arrivalTime,
                    ps -> {
                        Data data = (Data) ps[2];
                        int size = data.getSizeInBits();
                        if (isConnected()) {
                            ((Node) ps[0]).handleData((Node) ps[1], data);
                            bitsSent += size;
                        } else {
                            bitsDiscarded += size;
                        }
                    },
                    destination, Node.this, d);
            return transmitTimeInUs;
        }
    }
}

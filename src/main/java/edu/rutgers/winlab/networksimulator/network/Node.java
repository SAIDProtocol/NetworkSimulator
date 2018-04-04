package edu.rutgers.winlab.networksimulator.network;

import edu.rutgers.winlab.networksimulator.common.Data;
import edu.rutgers.winlab.networksimulator.common.PrioritizedQueue;
import edu.rutgers.winlab.networksimulator.common.QueuePoller;
import edu.rutgers.winlab.networksimulator.common.Timeline;
import edu.rutgers.winlab.networksimulator.common.Tuple1;
import edu.rutgers.winlab.networksimulator.common.Tuple2;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

/**
 * Represents a physical node in the network
 *
 * @author Jiachen Chen
 */
public abstract class Node {

    public static final long BW_IN_KBPS = Data.K_BIT / Timeline.MS_IN_SECOND;
    public static final long BW_IN_MBPS = Data.M_BIT / Timeline.MS_IN_SECOND;

    public static void linkNodes(Node n1, Node n2, int bwInBitsPerMs, long delayInMs, PrioritizedQueue queueN1N2, PrioritizedQueue queueN2N1) {
        n1.link(n2, bwInBitsPerMs, delayInMs, queueN1N2);
        n2.link(n1, bwInBitsPerMs, delayInMs, queueN2N1);
    }

    public static void disconnectNodes(Node n1, Node n2) {
        n1.disconnect(n2);
        n2.disconnect(n1);
    }

    protected static void sendData(Link l, Data d, boolean prioritized) {
        l.enQueue(d, prioritized);
    }

    private final String name;
    private final HashMap<Node, Link> links = new HashMap<>();
    private final ArrayList<Link> abortedLinks = new ArrayList<>();
    private final QueuePoller<Tuple2<Node, Data>> incomingQueue;
    private final HashMap<Node, Tuple1<Long>> bitsDiscarded = new HashMap<>();

    public Node(String name, PrioritizedQueue<Tuple2<Node, Data>> incomingQueue) {
        this.name = name;
        this.incomingQueue = new QueuePoller<>(this::handleData, incomingQueue);
    }

    public String getName() {
        return name;
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

    protected abstract long handleData(Tuple2<Node, Data> t);

    protected void sendData(Node destination, Data d, boolean prioritized) {
        Link l = links.get(destination);
        if (l == null) {
            throw new IllegalArgumentException(String.format("%s is not linked with %s", name, destination.name));
        }
        sendData(l, d, prioritized);
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

    private void addBitsDiscarded(Tuple2<Node, Data> t) {
        Node n = t.getV1();
        Tuple1<Long> val = bitsDiscarded.get(n);
        if (val == null) {
            bitsDiscarded.put(n, val = new Tuple1<>(0L));
        }
        val.setV1(val.getV1() + t.getV2().getSizeInBits());
    }

    private void handleIncomingData(Node source, Data d) {
        incomingQueue.enQueue(new Tuple2<>(source, d), false, this::addBitsDiscarded);
    }

    public class Link {

        private final Node destination;
        private final int bwBitsPerMS;
        private final long delayInUS;
        private final QueuePoller<Data> queuePoller;
        private long bitsSent = 0, bitsDiscarded = 0;
        private boolean connected = true;

        public Link(Node destination, int bwBitsPerMS, long delayInMS, PrioritizedQueue<Data> queue) {
            this.destination = destination;
            this.bwBitsPerMS = bwBitsPerMS;
            this.delayInUS = delayInMS * Timeline.US_IN_MS;
            queuePoller = new QueuePoller<>(this::handleData, queue);
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
            return queuePoller.isBusy();
        }

        public boolean isConnected() {
            return connected;
        }

        private void addDiscardedPacket(Data d) {
            bitsDiscarded += d.getSizeInBits();
        }

        public void enQueue(Data data, boolean prioritized) {
            queuePoller.enQueue(data, prioritized, this::addDiscardedPacket);
        }

        public void abort() {
            connected = false;
            queuePoller.clear(this::addDiscardedPacket);
        }

        private long handleData(Data data) {
            int sizeInBits = data.getSizeInBits();
            long transmitTimeInUs = sizeInBits * Timeline.US_IN_MS / bwBitsPerMS;
            long now = Timeline.nowInUs();
            long arrivalTime = now + transmitTimeInUs + delayInUS;
            Timeline.addEvent(arrivalTime,
                    ps -> {
                        Data dt = (Data) ps[2];
                        if (isConnected()) {
                            ((Node) ps[0]).handleIncomingData((Node) ps[1], dt);
                            bitsSent += dt.getSizeInBits();
                        } else {
                            addDiscardedPacket(dt);
                        }
                    }, destination, Node.this, data);
            return transmitTimeInUs;
        }
    }
}

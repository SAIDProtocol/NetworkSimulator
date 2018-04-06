package edu.rutgers.winlab.networksimulator.network;

import edu.rutgers.winlab.networksimulator.common.Data;
import edu.rutgers.winlab.networksimulator.common.PrioritizedQueue;
import edu.rutgers.winlab.networksimulator.common.QueuePoller;
import edu.rutgers.winlab.networksimulator.common.Timeline;
import edu.rutgers.winlab.networksimulator.common.Tuple1;
import edu.rutgers.winlab.networksimulator.common.Tuple2;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Represents a physical node in the network
 *
 * @author Jiachen Chen
 */
public abstract class Node {

    // 1kbps = 1,000 bits per 1,000 s = 1 bit per ms
    public static final int BW_IN_KBPS = (int) (Data.K_BIT / Timeline.MS_IN_SECOND);
    // 1Mbps = 1,000,000 bits per 1,000 s = 1,000 bits per ms
    public static final int BW_IN_MBPS = (int) (Data.M_BIT / Timeline.MS_IN_SECOND);

    public static void linkNodes(Node n1, Node n2,
            int bwInBitsPerMs, long delayInUS,
            PrioritizedQueue<Data> queueN1N2, PrioritizedQueue<Data> queueN2N1) {
        n1.link(n2, bwInBitsPerMs, delayInUS, queueN1N2);
        n2.link(n1, bwInBitsPerMs, delayInUS, queueN2N1);
    }

    public static Tuple2<Link, Link> disconnectNodes(Node n1, Node n2) {
        Link l1 = n1.disconnect(n2);
        Link l2 = n2.disconnect(n1);
        return new Tuple2<>(l1, l2);
    }

    protected static void sendData(Link l, Data d, boolean prioritized) {
        l.enQueue(d, prioritized);
    }

    private final String name;
    private final HashMap<Node, Link> links = new HashMap<>();
    private final QueuePoller<Tuple2<Node, Data>> incomingQueue;
    private final HashMap<Node, Tuple1<Long>> bitsDiscarded = new HashMap<>();

    public Node(String name, PrioritizedQueue<Tuple2<Node, Data>> incomingQueue) {
        this.name = name;
        this.incomingQueue = new QueuePoller<>(this::handleData, incomingQueue, t -> {
        });
    }

    public String getName() {
        return name;
    }

    public void forEachLink(Consumer<? super Link> consumer) {
        links.values().forEach(consumer);
    }

    public Stream<Link> linkStream() {
        return links.values().stream();
    }

    public void forEachBitsDiscarded(BiConsumer<? super Node, ? super Long> c) {
        bitsDiscarded.forEach((n, v) -> c.accept(n, v.getV1()));
    }

    public Stream<Tuple2<Node, Long>> bitsDiscardedStream() {
        return bitsDiscarded.entrySet().stream().map(e -> new Tuple2<>(e.getKey(), e.getValue().getV1()));
    }

    public Link link(Node another, int bwInBitsPerMs, long delayInUs, PrioritizedQueue<Data> queue) {
        if (isLinkedWith(another)) {
            throw new IllegalArgumentException(String.format("%s is already linked to %s.", name, another.name));
        }
        Link ret = new Link(another, bwInBitsPerMs, delayInUs, queue);
        links.put(another, ret);
        return ret;
    }

    public Link disconnect(Node another) {
        Link l = links.remove(another);
        if (l != null) {
            l.abort();
        }
        return l;
    }

    public boolean isLinkedWith(Node another) {
        return links.containsKey(another);
    }

    protected void sendData(Node destination, Data d, boolean prioritized) {
        Link l = links.get(destination);
        if (l == null) {
            throw new IllegalArgumentException(String.format("%s is not linked with %s", name, destination.name));
        }
        sendData(l, d, prioritized);
    }

    protected abstract long handleData(Tuple2<Node, Data> t);

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
        private final ArrayList<Consumer<? super Link>> idleHandlers = new ArrayList<>();

        public Link(Node destination, int bwBitsPerMS, long delayInUS, PrioritizedQueue<Data> queue) {
            this.destination = destination;
            this.bwBitsPerMS = bwBitsPerMS;
            this.delayInUS = delayInUS;
            queuePoller = new QueuePoller<>(this::handleData, queue, this::delayFireIdleEvent);
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

        public void enQueue(Data data, boolean prioritized) {
            queuePoller.enQueue(data, prioritized, this::addDiscardedPacket);
        }

        public void abort() {
            connected = false;
            queuePoller.clear(this::addDiscardedPacket);
        }

        public boolean addIdleHandler(Consumer<? super Link> e) {
            return idleHandlers.add(e);
        }

        public boolean removeIdleHandler(Consumer<? super Link> e) {
            return idleHandlers.remove(e);
        }

        private void addDiscardedPacket(Data d) {
            bitsDiscarded += d.getSizeInBits();
        }

        // ps[0]: data
        private void processDataArrival(Object... ps) {
            Data dt = (Data) ps[0];
            if (isConnected()) {
                destination.handleIncomingData(Node.this, dt);
                bitsSent += dt.getSizeInBits();
            } else {
                addDiscardedPacket(dt);
            }
        }

        private long handleData(Data data) {
            long transmitTimeInUs = data.getSizeInBits() * Timeline.US_IN_MS / bwBitsPerMS;
            Timeline.addEvent(Timeline.nowInUs() + transmitTimeInUs + delayInUS, this::processDataArrival, data);
            return transmitTimeInUs;
        }

        private void delayFireIdleEvent(Object... params) {
            Timeline.addEvent(Timeline.nowInUs() + delayInUS, this::fireIdleEvent);
        }

        private void fireIdleEvent(Object... params) {
            idleHandlers.forEach(idleHandler -> idleHandler.accept(this));
        }

    }
}

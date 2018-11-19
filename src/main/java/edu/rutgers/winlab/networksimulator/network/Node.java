package edu.rutgers.winlab.networksimulator.network;

import edu.rutgers.winlab.networksimulator.common.Data;
import edu.rutgers.winlab.networksimulator.common.PrioritizedQueue;
import edu.rutgers.winlab.networksimulator.common.QueuePoller;
import edu.rutgers.winlab.networksimulator.common.Timeline;
import edu.rutgers.winlab.networksimulator.common.Tuple2;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
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
        n1.unicastLink(n2, bwInBitsPerMs, delayInUS, queueN1N2);
        n2.unicastLink(n1, bwInBitsPerMs, delayInUS, queueN2N1);
    }

    public static Tuple2<UnicastLink, UnicastLink> disconnectNodes(Node n1, Node n2) {
        UnicastLink l1 = n1.unicastDisconnect(n2);
        UnicastLink l2 = n2.unicastDisconnect(n1);
        return new Tuple2<>(l1, l2);
    }

    public static long getTransmitTimeInUs(int dataSize, int bw) {
        return dataSize * Timeline.US_IN_MS / bw;
    }

    protected static void sendData(AbstractLink l, Data d, boolean prioritized) {
        l.enQueue(d, prioritized);
    }

    private final String name;
    private final HashMap<Node, UnicastLink> unicastLinks = new HashMap<>();
    private final HashMap<String, BroadcastLink> broadcastLinks = new HashMap<>();
    private final QueuePoller<Tuple2<Node, Data>> incomingQueue;
    // bits discarded due to incoming queue overflow
    // key: source node, value: bits discarded from the source
    private final HashMap<Node, Long> bitsDiscarded = new HashMap<>();

    public Node(String name, PrioritizedQueue<Tuple2<Node, Data>> incomingQueue) {
        this.name = name;
        this.incomingQueue = new QueuePoller<>(this::handleData, incomingQueue, t -> {
        });
    }

    public String getName() {
        return name;
    }

    public void forEachUnicastLink(Consumer<? super UnicastLink> consumer) {
        unicastLinks.values().forEach(consumer);
    }

    public Stream<UnicastLink> unicastLinkStream() {
        return unicastLinks.values().stream();
    }

    public UnicastLink unicastLink(Node another, int bwInBitsPerMs, long delayInUs, PrioritizedQueue<Data> queue) {
        if (isUnicastLinkedWith(another)) {
            throw new IllegalArgumentException(String.format("%s is already linked to %s.", name, another.name));
        }
        UnicastLink ret = new UnicastLink(another, bwInBitsPerMs, delayInUs, queue);
        unicastLinks.put(another, ret);
        return ret;
    }
    
    public UnicastLink getUnicastLink(Node another) {
        if (!isUnicastLinkedWith(another)) {
            throw new IllegalArgumentException(String.format("%s is not linked to %s.", name, another.name));
        }
        return unicastLinks.get(another);
    }

    public UnicastLink unicastDisconnect(Node another) {
        UnicastLink l = unicastLinks.remove(another);
        if (l != null) {
            l.abort();
        }
        return l;
    }

    public boolean isUnicastLinkedWith(Node another) {
        return unicastLinks.containsKey(another);
    }

    protected void sendUnicastData(Node destination, Data d, boolean prioritized) {
        UnicastLink l = unicastLinks.get(destination);
        if (l == null) {
            throw new IllegalArgumentException(String.format("%s is not linked with %s", name, destination.name));
        }
        sendData(l, d, prioritized);
    }

    public boolean createBroadcastChannel(String name, int bwBitsPerMS, long delayInUS, PrioritizedQueue<Data> queue) {
        if (broadcastLinks.containsKey(name)) {
            return false;
        }
        broadcastLinks.put(name, new BroadcastLink(bwBitsPerMS, delayInUS, queue));
        return true;
    }

    public BroadcastLink getBroadcastChannel(String name) {
        return broadcastLinks.get(name);
    }

    public void forEachBroadcastLink(BiConsumer<? super String, ? super BroadcastLink> consumer) {
        broadcastLinks.forEach(consumer);

    }

    public Stream<Entry<String, BroadcastLink>> broadcastLinkStream() {
        return broadcastLinks.entrySet().stream();
    }

    protected void sendBroadcastData(String name, Data d, boolean prioritized) {
        BroadcastLink l = broadcastLinks.get(name);
        if (l == null) {
            throw new IllegalArgumentException(String.format("%s does not have broadcast link %s", this.name, name));
        }
        sendData(l, d, prioritized);
    }

    protected abstract long handleData(Tuple2<Node, Data> t);

    public void forEachBitsDiscarded(BiConsumer<? super Node, ? super Long> c) {
        bitsDiscarded.forEach((n, v) -> c.accept(n, v));
    }

    public Stream<Tuple2<Node, Long>> bitsDiscardedStream() {
        return bitsDiscarded.entrySet().stream().map(e -> new Tuple2<>(e.getKey(), e.getValue()));
    }

    private void addBitsDiscarded(Tuple2<Node, Data> t) {
        bitsDiscarded.merge(t.getV1(), (long) t.getV2().getSizeInBits(), Long::sum);
    }

    protected void enqueueIncomingData(Node source, Data d, boolean prioritized) {
        incomingQueue.enQueue(new Tuple2<>(source, d), prioritized, this::addBitsDiscarded);
    }

    public abstract class AbstractLink {

        private final int bwBitsPerMS;
        private final long delayInUS;
        private final QueuePoller<Data> queuePoller;
        private long bitsSent = 0, bitsDiscarded = 0;
        private boolean connected = true;
        private final ArrayList<Consumer<? super AbstractLink>> idleHandlers = new ArrayList<>();

        public AbstractLink(int bwBitsPerMS, long delayInUS, PrioritizedQueue<Data> queue) {
            this.bwBitsPerMS = bwBitsPerMS;
            this.delayInUS = delayInUS;
            queuePoller = new QueuePoller<>(this::handleData, queue, this::delayFireIdleEvent);
        }

        protected abstract long handleData(Data d);

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

        public boolean addIdleHandler(Consumer<? super AbstractLink> e) {
            return idleHandlers.add(e);
        }

        public boolean removeIdleHandler(Consumer<? super AbstractLink> e) {
            return idleHandlers.remove(e);
        }

        protected long getTransmitTimeInUs(Data data) {
            return Node.getTransmitTimeInUs(data.getSizeInBits(), bwBitsPerMS);
        }

        protected void addDiscardedPacket(Data d) {
            bitsDiscarded += d.getSizeInBits();
        }

        protected void addSentPacket(Data d) {
            bitsSent += d.getSizeInBits();
        }

        private void delayFireIdleEvent(Object... params) {
            Timeline.addEvent(Timeline.nowInUs() + delayInUS, this::fireIdleEvent);
        }

        private void fireIdleEvent(Object... params) {
            idleHandlers.forEach(idleHandler -> idleHandler.accept(this));
        }
    }

    public class UnicastLink extends AbstractLink {

        private final Node destination;

        public UnicastLink(Node destination, int bwBitsPerMS, long delayInUS, PrioritizedQueue<Data> queue) {
            super(bwBitsPerMS, delayInUS, queue);
            this.destination = destination;
        }

        public Node getDestination() {
            return destination;
        }

        // ps[0]: data
        private void processDataArrival(Object... ps) {
            Data dt = (Data) ps[0];
            if (isConnected()) {
                destination.enqueueIncomingData(Node.this, dt, false);
                addSentPacket(dt);
            } else {
                addDiscardedPacket(dt);
            }
        }

        @Override
        protected long handleData(Data data) {
            long transmitTimeInUs = getTransmitTimeInUs(data);
            Timeline.addEvent(Timeline.nowInUs() + transmitTimeInUs + getDelayInUS(), this::processDataArrival, data);
            return transmitTimeInUs;
        }
    }

    public class BroadcastLink extends AbstractLink {

        private final HashSet<Node> destinations = new HashSet<>();
        private final HashMap<Data, HashSet<Node>> packetsInFlight = new HashMap<>();

        public BroadcastLink(int bwBitsPerMS, long delayInUS, PrioritizedQueue<Data> queue) {
            super(bwBitsPerMS, delayInUS, queue);
        }

        public boolean addNode(Node n) {
            return destinations.add(n);
        }

        public boolean removeNode(Node n, boolean needDiscardDataInFlight) {
            boolean removed = destinations.remove(n);
            if (removed && needDiscardDataInFlight) {
                packetsInFlight.forEach((d, ns) -> {
                    if (ns.remove(n)) {
                        addDiscardedPacket(d);
                    }
                });
            }
            return removed;
        }

        private void processDataArrival(Object... ps) {
            Data dt = (Data) ps[0];
            HashSet<Node> packetDestinations = packetsInFlight.remove(dt);
            if (isConnected()) {
                packetDestinations.forEach(destination -> {
                    destination.enqueueIncomingData(Node.this, dt, false);
                    addSentPacket(dt);
                });
            } else {
                packetDestinations.forEach(destination -> {
                    addDiscardedPacket(dt);
                });
            }
        }

        @Override
        protected long handleData(Data data) {
            long transmitTimeInUs = getTransmitTimeInUs(data);
            packetsInFlight.put(data, new HashSet<>(destinations));
            Timeline.addEvent(Timeline.nowInUs() + transmitTimeInUs + getDelayInUS(), this::processDataArrival, data);
            return transmitTimeInUs;
        }

    }
}

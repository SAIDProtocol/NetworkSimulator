package edu.rutgers.winlab.networksimulator.common;

import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 *
 * @author Jiachen Chen
 * @param <T>
 */
public class QueuePoller<T> {

    private final BiFunction<Data, T, Long> dataHandler;
    private final PrioritizedQueue queue;
    private boolean busy = false;

    public QueuePoller(BiFunction<Data, T, Long> dataHandler, PrioritizedQueue queue) {
        this.dataHandler = dataHandler;
        this.queue = queue;
    }

    public long enQueue(Data d, T val, boolean prioritized) {
        long ret = queue.enQueue(d, val, prioritized);
        if (!busy) {
            busy = true;
            runQueue();
        }
        return ret;
    }

    private void runQueue(Object... params) {
        Tuple2<Data, T> d = queue.deQueue();
        if (d == null) {
            busy = false;
        } else {
            long now = Timeline.nowInUs();
            long v = dataHandler.apply(d.getV1(), d.getV2());
            Timeline.addEvent(now + v, this::runQueue);
        }
    }

    public boolean isBusy() {
        return busy;
    }

    public long clear() {
        return queue.clear();
    }

    public long getSizeInBits() {
        return queue.getSizeInBits();
    }

    public Stream<Tuple2<Data, T>> stream() {
        return queue.stream();
    }
}

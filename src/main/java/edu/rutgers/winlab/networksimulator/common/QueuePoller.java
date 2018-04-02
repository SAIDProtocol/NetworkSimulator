package edu.rutgers.winlab.networksimulator.common;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 *
 * @author jiachen
 */
public class QueuePoller {

    private final Function<Data, Long> dataHandler;
    private final PrioritizedQueue queue;
    private boolean busy = false;

    public QueuePoller(Function<Data, Long> dataHandler, PrioritizedQueue queue) {
        this.dataHandler = dataHandler;
        this.queue = queue;
    }

    public long enQueue(Data d, boolean prioritized) {
        long ret = queue.enQueue(d, prioritized);
        if (!busy) {
            busy = true;
            runQueue();
        }
        return ret;
    }

    public long clear() {
        return queue.clear();
    }

    public long getSizeInBits() {
        return queue.getSizeInBits();
    }

    public Stream<Data> stream() {
        return queue.stream();
    }
    
    private void runQueue(Object... params) {
        Data d = queue.deQueue();
        if (d == null) {
            busy = false;
        } else {
            long now = Timeline.nowInUs();
            long v = dataHandler.apply(d);
            Timeline.addEvent(now + v, this::runQueue);
        }
    }

}

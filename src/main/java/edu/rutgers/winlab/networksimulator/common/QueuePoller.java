package edu.rutgers.winlab.networksimulator.common;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 *
 * @author Jiachen Chen
 * @param <T>
 */
public class QueuePoller<T> {

    private final Function<T, Long> dataHandler;
    private final PrioritizedQueue<T> queue;
    private boolean busy = false;
    private final Consumer<? super QueuePoller<T>> idleHandler;

    public QueuePoller(Function<T, Long> dataHandler, PrioritizedQueue<T> queue, Consumer<? super QueuePoller<T>> idleHandler) {
        this.dataHandler = dataHandler;
        this.queue = queue;
        this.idleHandler = idleHandler;
    }

    public void enQueue(T val, boolean prioritized) {
        queue.enQueue(val, prioritized);
        if (!busy) {
            busy = true;
            runQueue();
        }
    }
    
    public void enQueue(T val, boolean prioritized, Consumer<T> consumer) {
        queue.enQueue(val, prioritized, consumer);
        if (!busy) {
            busy = true;
            runQueue();
        }
    }

    private void runQueue(Object... params) {
        T val = queue.deQueue();
        if (val == null) {
            busy = false;
            idleHandler.accept(this);
        } else {
            long now = Timeline.nowInUs();
            long v = dataHandler.apply(val);
            Timeline.addEvent(now + v, this::runQueue);
        }
    }

    public boolean isBusy() {
        return busy;
    }

    public void clear() {
        queue.clear();
    }
    
    public void clear(Consumer<T> consumer) {
        queue.clear(consumer);
    }

    public int getSize() {
        return queue.getSize();
    }

    public Stream<T> stream() {
        return queue.stream();
    }


}

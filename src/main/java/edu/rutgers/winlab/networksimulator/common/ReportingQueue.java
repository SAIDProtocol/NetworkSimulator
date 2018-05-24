package edu.rutgers.winlab.networksimulator.common;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 * @author jiachen
 * @param <V>
 * @param <T>
 */
public class ReportingQueue<V, T extends PrioritizedQueue<V>> implements PrioritizedQueue<V> {

    private final String name;
    private final PrioritizedQueue<V> innerQueue;
    private final BiConsumer<String, Integer> sizeChangedHandler;

    public ReportingQueue(String name, PrioritizedQueue<V> innerQueue, BiConsumer<String, Integer> sizeChangedHandler) {
        this.name = name;
        this.innerQueue = innerQueue;
        this.sizeChangedHandler = sizeChangedHandler;
    }

    public String getName() {
        return name;
    }
    
    private void fireSizeChanged() {
        sizeChangedHandler.accept(name, innerQueue.getSize());
    }

    @Override
    public void enQueue(V val, boolean prioritized) {
        innerQueue.enQueue(val, prioritized);
        fireSizeChanged();
    }

    @Override
    public void enQueue(V val, boolean prioritized, Consumer<? super V> consumer) {
        innerQueue.enQueue(val, prioritized, consumer);
        fireSizeChanged();
    }

    @Override
    public V deQueue() {
        V ret = innerQueue.deQueue();
        fireSizeChanged();
        return ret;
    }

    @Override
    public void clear(Consumer<? super V> consumer) {
        innerQueue.clear(consumer);
        fireSizeChanged();
    }

    @Override
    public void clear() {
        innerQueue.clear();
        fireSizeChanged();
    }

    @Override
    public int getSize() {
        return getSize();
    }

    @Override
    public Stream<V> stream() {
        return innerQueue.stream();
    }

}

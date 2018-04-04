package edu.rutgers.winlab.networksimulator.common;

import java.util.ArrayDeque;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 * @author Jiachen Chen
 * @param <T>
 */
public class UnlimitedQueue<T> implements PrioritizedQueue<T> {

    protected final ArrayDeque<T> normalQueue = new ArrayDeque<>(),
            priorityQueue = new ArrayDeque<>();
    protected int size = 0;

    @Override
    public void enQueue(T val, boolean prioritized) {
        if (prioritized) {
            priorityQueue.offer(val);
        } else {
            normalQueue.offer(val);
        }
        size++;
    }

    @Override
    public void enQueue(T val, boolean prioritized, Consumer<T> consumer) {
        enQueue(val, prioritized);
    }

    @Override
    public T deQueue() {
        T val = priorityQueue.poll();
        val = (val == null) ? normalQueue.poll() : val;
        size -= (val == null) ? 0 : 1;
        return val;
    }

    @Override
    public void clear(Consumer<T> consumer) {
        priorityQueue.forEach(consumer);
        normalQueue.forEach(consumer);
        clear();
    }

    @Override
    public void clear() {
        normalQueue.clear();
        priorityQueue.clear();
        size = 0;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public Stream<T> stream() {
        return Stream.concat(priorityQueue.stream(), normalQueue.stream());
    }
}

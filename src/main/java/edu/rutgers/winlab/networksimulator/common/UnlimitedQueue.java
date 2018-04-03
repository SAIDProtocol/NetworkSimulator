package edu.rutgers.winlab.networksimulator.common;

import java.util.ArrayDeque;
import java.util.stream.Stream;

/**
 *
 * @author Jiachen Chen
 * @param <T>
 */
public class UnlimitedQueue<T> implements PrioritizedQueue<T> {

    protected final ArrayDeque<Tuple2<Data, T>> normalQueue = new ArrayDeque<>(),
            priorityQueue = new ArrayDeque<>();
    protected long sizeInBits = 0;

    @Override
    public long enQueue(Data d, T val, boolean prioritized) {
        if (prioritized) {
            priorityQueue.offer(new Tuple2<>(d, val));
        } else {
            normalQueue.offer(new Tuple2<>(d, val));
        }
        sizeInBits += d.getSizeInBits();
        return 0;
    }

    @Override
    public Tuple2<Data, T> deQueue() {
        Tuple2<Data, T> d = priorityQueue.poll();
        if (d == null) {
            d = normalQueue.poll();
        }
        sizeInBits -= (d == null) ? 0 : d.getV1().getSizeInBits();
        return d;
    }

    @Override
    public long clear() {
        normalQueue.clear();
        priorityQueue.clear();
        long ret = sizeInBits;
        sizeInBits = 0;
        return ret;
    }

    @Override
    public long getSizeInBits() {
        return sizeInBits;
    }

    @Override
    public Stream<Tuple2<Data, T>> stream() {
        return Stream.concat(priorityQueue.stream(), normalQueue.stream());
    }

}

package edu.rutgers.winlab.networksimulator.common;

import java.util.ArrayDeque;
import java.util.stream.Stream;

/**
 *
 * @author Jiachen Chen
 */
public class UnlimitedQueue implements PrioritizedQueue {

    protected final ArrayDeque<Data> normalQueue = new ArrayDeque<>(),
            priorityQueue = new ArrayDeque<>();
    protected long sizeInBits = 0;

    @Override
    public long enQueue(Data d, boolean prioritized) {
        if (prioritized) {
            priorityQueue.offer(d);
        } else {
            normalQueue.offer(d);
        }
        sizeInBits += d.getSizeInBits();
        return 0;
    }

    @Override
    public Data deQueue() {
        Data d = priorityQueue.poll();
        if (d == null) {
            d = normalQueue.poll();
        }
        sizeInBits -= (d == null) ? 0 : d.getSizeInBits();
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
    public Stream<Data> stream() {
        return Stream.concat(priorityQueue.stream(), normalQueue.stream());
    }

}

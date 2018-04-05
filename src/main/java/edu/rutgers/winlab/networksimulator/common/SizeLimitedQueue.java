package edu.rutgers.winlab.networksimulator.common;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 *
 * @author Jiachen Chen
 * @param <T>
 */
public class SizeLimitedQueue<T> extends UnlimitedQueue<T> {

    private final long capacityInBits;
    private final Function<? super T, Integer> contentSizeConverter;
    private long sizeInBits = 0;

    public SizeLimitedQueue(long capacityInBits, Function<? super T, Integer> contentSizeConverter) {
        this.capacityInBits = capacityInBits;
        this.contentSizeConverter = contentSizeConverter;
    }

    @Override
    public void enQueue(T val, boolean prioritized) {
        int sz = contentSizeConverter.apply(val);
        long requiredSize = capacityInBits - sz;
        // we have enough space to place the data
        if (sizeInBits <= requiredSize) {
            super.enQueue(val, prioritized);
            sizeInBits += sz;
            return;
        }
        // if the target is not prioritized, and we don't have space, discard it
        if (!prioritized) {
            return;
        }
        // remove some normal data to fit the prioritized data in
        while (sizeInBits > requiredSize) {
            T d = normalQueue.pollLast();
            // now we have drained the normal queue, but we still cannot fit in
            // the prioritized data, discard it and all the data in the normal queue
            if (d == null) {
                return;
            }
            int tmpSz = contentSizeConverter.apply(d);
            sizeInBits -= tmpSz;
            size--;
        }
        // now we have enough space to place the prioritized data
        priorityQueue.offer(val);
        sizeInBits += sz;
        size++;
    }

    @Override
    public void enQueue(T val, boolean prioritized, Consumer<? super T> consumer) {
        int sz = contentSizeConverter.apply(val);
        long requiredSize = capacityInBits - sz;
        // we have enough space to place the data
        if (sizeInBits <= requiredSize) {
            super.enQueue(val, prioritized);
            sizeInBits += sz;
            return;
        }
        // if the target is not prioritized, and we don't have space, discard it
        if (!prioritized) {
            consumer.accept(val);
            return;
        }
        // remove some normal data to fit the prioritized data in
        while (sizeInBits > requiredSize) {
            T d = normalQueue.pollLast();
            // now we have drained the normal queue, but we still cannot fit in
            // the prioritized data, discard it and all the data in the normal queue
            if (d == null) {
                consumer.accept(val);
                return;
            }
            int tmpSz = contentSizeConverter.apply(d);
            sizeInBits -= tmpSz;
            size--;
            consumer.accept(d);
        }
        // now we have enough space to place the prioritized data
        priorityQueue.offer(val);
        sizeInBits += sz;
        size++;
    }

    @Override
    public T deQueue() {
        T d = super.deQueue();
        if (d != null) {
            sizeInBits -= contentSizeConverter.apply(d);
        }
        return d;
    }

    @Override
    public void clear() {
        super.clear();
        sizeInBits = 0;
    }

    public long getCapacityInBits() {
        return capacityInBits;
    }

    public long getSizeInBits() {
        return sizeInBits;
    }
}

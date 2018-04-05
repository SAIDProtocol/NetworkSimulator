package edu.rutgers.winlab.networksimulator.common;

import java.util.function.Consumer;

/**
 *
 * @author Jiachen Chen
 * @param <T>
 */
public class ItemLimitedQueue<T> extends UnlimitedQueue<T> {

    private final int capacityInItems;

    public ItemLimitedQueue(int capacityInItems) {
        this.capacityInItems = capacityInItems;
    }

    @Override
    public void enQueue(T val, boolean prioritized) {
        if (size < capacityInItems) {
            super.enQueue(val, prioritized);
            return;
        }
        // if the target is not prioritized, and we don't have space, discard it
        if (!prioritized) {
            return;
        }
        T d = normalQueue.pollLast();
        // now we have drained the normal queue, but we still cannot fit in
        // the prioritized data, discard it and all the data in the normal queue
        if (d == null) {
            return;
        }
        priorityQueue.offer(val);
    }

    @Override
    public void enQueue(T val, boolean prioritized, Consumer<? super T> consumer) {
        if (size < capacityInItems) {
            super.enQueue(val, prioritized);
            return;
        }
        // if the target is not prioritized, and we don't have space, discard it
        if (!prioritized) {
            consumer.accept(val);
            return;
        }
        T d = normalQueue.pollLast();
        // now we have drained the normal queue, but we still cannot fit in
        // the prioritized data, discard it and all the data in the normal queue
        if (d == null) {
            consumer.accept(val);
            return;
        }
        consumer.accept(d);
        priorityQueue.offer(val);
    }

    public int getCapacityInItems() {
        return capacityInItems;
    }

}

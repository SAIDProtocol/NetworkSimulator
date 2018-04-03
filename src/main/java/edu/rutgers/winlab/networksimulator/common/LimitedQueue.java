package edu.rutgers.winlab.networksimulator.common;

/**
 *
 * @author Jiachen Chen
 * @param <T>
 */
public class LimitedQueue<T> extends UnlimitedQueue<T> {

    private final long capacityInBits;

    public LimitedQueue(long capacityInBits) {
        this.capacityInBits = capacityInBits;
    }

    @Override
    public long enQueue(Data d, T val, boolean prioritized) {
        int sz = d.getSizeInBits();
        long requiredSize = capacityInBits - sz;
        // we have enough space to place the data
        if (sizeInBits <= requiredSize) {
            return super.enQueue(d, val, prioritized);
        }
        // if the target is not prioritized, and we don't have space, discard it
        if (!prioritized) {
            return sz;
        }
        long ret = 0;
        // remove some normal data to fit the prioritized data in
        while (sizeInBits > requiredSize) {
            Tuple2<Data, T> dx = normalQueue.pollLast();
            // now we have drained the normal queue, but we still cannot fit in
            // the prioritized data, discard it and all the data in the normal queue
            if (dx == null) {
                return ret + sz;
            }
            int tmpSz = dx.getV1().getSizeInBits();
            sizeInBits -= tmpSz;
            ret += tmpSz;
        }
        // now we have enough space to place the prioritized data
        priorityQueue.offer(new Tuple2<>(d, val));
        sizeInBits += sz;
        return ret;
    }

}

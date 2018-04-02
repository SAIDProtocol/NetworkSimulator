package edu.rutgers.winlab.networksimulator.common;

import java.util.stream.Stream;

/**
 *
 * @author Jiachen Chen
 */
public interface PrioritizedQueue {

    /**
     * Adds a data into the queue.
     *
     * @param d the data to be added into the queue.
     * @param prioritized if the data should be added as prioritized.
     * @return the number of BITS discarded from the queue.
     */
    public long enQueue(Data d, boolean prioritized);

    /**
     * Retrieves a data from the queue.
     *
     * @return the data at the head of the queue, null if queue is empty.
     */
    public Data deQueue();

    /**
     * Clears all the items in the queue.
     *
     * @return the number of BITS cleared from the queue.
     */
    public long clear();

    /**
     * Gets the current size of the queue.
     *
     * @return the number of BITS currently in the queue.
     */
    public long getSizeInBits();

    /**
     * Gets the stream of the queue.
     *
     * @return
     */
    public Stream<Data> stream();
}

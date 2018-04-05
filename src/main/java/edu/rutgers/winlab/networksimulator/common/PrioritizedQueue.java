package edu.rutgers.winlab.networksimulator.common;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 * @author Jiachen Chen
 * @param <T> Additional information with data
 */
public interface PrioritizedQueue<T> {

    /**
     * Adds a data into the queue.
     *
     * @param val additional information with the data
     * @param prioritized if the data should be added as prioritized.
     */
    public void enQueue(T val, boolean prioritized);

    /**
     * Adds a data into the queue.
     *
     * @param val additional information with the data
     * @param prioritized if the data should be added as prioritized.
     * @param consumer called on each entry before dropping
     */
    public void enQueue(T val, boolean prioritized, Consumer<? super T> consumer);
    
    /**
     * Retrieves a data from the queue.
     *
     * @return the data at the head of the queue, null if queue is empty.
     */
    public T deQueue();

    /**
     * Clears all the items in the queue.
     *
     * @param consumer will be called for each entry before deletion
     */
    public void clear(Consumer<? super T> consumer);

    /**
     * Clears all the items in the queue.
     */
    public void clear();

    /**
     * Gets the current size (# of items) of the queue.
     *
     * @return the number of entries currently in the queue.
     */
    public int getSize();

    /**
     * Gets the stream of the queue.
     *
     * @return
     */
    public Stream<T> stream();
}

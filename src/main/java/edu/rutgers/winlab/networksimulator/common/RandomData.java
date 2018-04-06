package edu.rutgers.winlab.networksimulator.common;

/**
 * Represents a set of random bits on wire.
 *
 * @author Jiachen Chen
 */
public class RandomData implements Data {

    private final int sizeInBits;

    public RandomData(int sizeInBits) {
        this.sizeInBits = sizeInBits;
    }

    @Override
    public int getSizeInBits() {
        return sizeInBits;
    }
}

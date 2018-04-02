package edu.rutgers.winlab.networksimulator.common;

/**
 * Represents any data that can be transmitted in the network.
 *
 * @author Jiachen Chen
 */
public interface Data {
    
    public static int BIT = 1;
    public static int K_BIT = 1000 * BIT;
    public static int M_BIT = 1000 * K_BIT;
    public static int BYTE = 8 * BIT;
    public static int K_BYTE = 1024 * BYTE;
    public static int M_BYTE = 1024 * K_BYTE;

    public int getSizeInBits();

    public Data copy();
}

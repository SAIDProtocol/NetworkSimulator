package edu.rutgers.winlab.networksimulator.network.mf.graphpubsub.packets;

import edu.rutgers.winlab.networksimulator.common.RandomData;

/**
 *
 * @author Jiachen Chen
 */
public class SerialData extends RandomData {

    private final int id;

    public SerialData(int id, int sizeInBits) {
        super(sizeInBits);
        this.id = id;
    }

    public int getId() {
        return id;
    }
}

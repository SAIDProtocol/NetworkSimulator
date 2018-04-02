package edu.rutgers.winlab.networksimulator.common;

import java.util.Objects;

/**
 *
 * @author Jiachen Chen
 * @param <T>
 */
public class Tuple1<T> {

    T v1;

    public Tuple1(T v1) {
        this.v1 = v1;
    }

    public T getV1() {
        return v1;
    }

    public void setV1(T v1) {
        this.v1 = v1;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.v1);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Tuple1<?> other = (Tuple1<?>) obj;
        return Objects.equals(this.v1, other.v1);
    }

}

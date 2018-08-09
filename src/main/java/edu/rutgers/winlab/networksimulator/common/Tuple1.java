package edu.rutgers.winlab.networksimulator.common;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 *
 * @author Jiachen Chen
 * @param <T>
 */
public class Tuple1<T> {

    private T v1;

    public Tuple1(T v1) {
        this.v1 = v1;
    }

    public T getV1() {
        return v1;
    }

    public void setV1(T v1) {
        this.v1 = v1;
    }
    
    public T mergeV1(T v, BiFunction<? super T, ? super T, ? extends T> remappingFunction) {
        this.v1 = remappingFunction.apply(this.v1, v);
        return this.v1;
    }

    public void setValues(T v1) {
        setV1(v1);
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

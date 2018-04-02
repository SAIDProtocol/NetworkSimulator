package edu.rutgers.winlab.networksimulator.common;

import java.util.Objects;

/**
 *
 * @author Jiachen Chen
 * @param <T1>
 * @param <T2>
 * @param <T3>
 * @param <T4>
 */
public class Tuple4<T1, T2, T3, T4> {

    private T1 v1;
    private T2 v2;
    private T3 v3;
    private T4 v4;

    public Tuple4(T1 v1, T2 v2, T3 v3, T4 v4) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.v4 = v4;
    }

    public T1 getV1() {
        return v1;
    }

    public void setV1(T1 v1) {
        this.v1 = v1;
    }

    public T2 getV2() {
        return v2;
    }

    public void setV2(T2 v2) {
        this.v2 = v2;
    }

    public T3 getV3() {
        return v3;
    }

    public void setV3(T3 v3) {
        this.v3 = v3;
    }

    public T4 getV4() {
        return v4;
    }

    public void setV4(T4 v4) {
        this.v4 = v4;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.v1);
        hash = 97 * hash + Objects.hashCode(this.v2);
        hash = 97 * hash + Objects.hashCode(this.v3);
        hash = 97 * hash + Objects.hashCode(this.v4);
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
        final Tuple4<?, ?, ?, ?> other = (Tuple4<?, ?, ?, ?>) obj;
        if (!Objects.equals(this.v1, other.v1)) {
            return false;
        }
        if (!Objects.equals(this.v2, other.v2)) {
            return false;
        }
        if (!Objects.equals(this.v3, other.v3)) {
            return false;
        }
        return Objects.equals(this.v4, other.v4);
    }

}

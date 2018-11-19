/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.networksimulator.common.function;

import java.util.Objects;
import java.util.function.DoubleToIntFunction;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongToIntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 *
 * @author jiachen
 */
public interface MyIntSupplier extends IntSupplier {

    default <V> Supplier<V> andThen(IntFunction<V> after) {
        Objects.requireNonNull(after);
        return () -> after.apply(getAsInt());
    }

    default MyDoubleSupplier andThen(IntToDoubleFunction after) {
        Objects.requireNonNull(after);
        return () -> after.applyAsDouble(getAsInt());
    }

    default MyIntSupplier andThen(IntUnaryOperator after) {
        Objects.requireNonNull(after);
        return () -> after.applyAsInt(getAsInt());
    }

    default MyLongSupplier andThen(IntToLongFunction after) {
        Objects.requireNonNull(after);
        return () -> after.applyAsLong(getAsInt());
    }

    default MyDoubleSupplier addFilter(IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        return () -> {
            int d;
            do {
                d = getAsInt();
            } while (!predicate.test(d));
            return d;
        };
    }

    default <V> ToIntFunction<V> toToIntFunction() {
        return o -> getAsInt();
    }

    default IntUnaryOperator toIntUnaryOperator() {
        return i -> getAsInt();
    }

    default DoubleToIntFunction toDoubleToIntFunction() {
        return d -> getAsInt();
    }

    default LongToIntFunction toLongToIntFunction() {
        return l -> getAsInt();
    }

}

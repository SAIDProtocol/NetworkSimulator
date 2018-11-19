/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.networksimulator.common.function;

import java.util.Objects;
import java.util.function.DoubleToLongFunction;
import java.util.function.IntToLongFunction;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

/**
 *
 * @author jiachen
 */
public interface MyLongSupplier extends LongSupplier {

    default <V> Supplier<V> andThen(LongFunction<V> after) {
        Objects.requireNonNull(after);
        return () -> after.apply(getAsLong());
    }

    default MyDoubleSupplier andThen(LongToDoubleFunction after) {
        Objects.requireNonNull(after);
        return () -> after.applyAsDouble(getAsLong());
    }

    default MyIntSupplier andThen(LongToIntFunction after) {
        Objects.requireNonNull(after);
        return () -> after.applyAsInt(getAsLong());
    }

    default MyLongSupplier andThen(LongUnaryOperator after) {
        Objects.requireNonNull(after);
        return () -> after.applyAsLong(getAsLong());
    }

    default MyDoubleSupplier addFilter(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        return () -> {
            long d;
            do {
                d = getAsLong();
            } while (!predicate.test(d));
            return d;
        };
    }

    default <V> ToLongFunction<V> toToLongFunction() {
        return o -> getAsLong();
    }

    default IntToLongFunction toIntToLongFunction() {
        return i -> getAsLong();
    }

    default DoubleToLongFunction toDoubleToLongFunction() {
        return d -> getAsLong();
    }

    default LongUnaryOperator toLongUnaryOperator() {
        return l -> getAsLong();
    }
}

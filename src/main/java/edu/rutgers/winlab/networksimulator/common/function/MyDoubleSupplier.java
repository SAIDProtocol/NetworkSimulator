/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.networksimulator.common.function;

import java.util.Objects;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntToDoubleFunction;
import java.util.function.LongToDoubleFunction;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/**
 *
 * @author jiachen
 */
@FunctionalInterface
public interface MyDoubleSupplier extends DoubleSupplier {

    default <V> Supplier<V> andThen(DoubleFunction<V> after) {
        Objects.requireNonNull(after);
        return () -> after.apply(getAsDouble());
    }

    default MyDoubleSupplier andThen(DoubleUnaryOperator after) {
        Objects.requireNonNull(after);
        return () -> after.applyAsDouble(getAsDouble());
    }

    default MyIntSupplier andThen(DoubleToIntFunction after) {
        Objects.requireNonNull(after);
        return () -> after.applyAsInt(getAsDouble());
    }

    default MyLongSupplier andThen(DoubleToLongFunction after) {
        Objects.requireNonNull(after);
        return () -> after.applyAsLong(getAsDouble());
    }

    default MyDoubleSupplier addFilter(DoublePredicate predicate) {
        Objects.requireNonNull(predicate);
        return () -> {
            double d;
            do {
                d = getAsDouble();
            } while (!predicate.test(d));
            return d;
        };
    }

    default <V> ToDoubleFunction<V> toToDoubleFunction() {
        return o -> getAsDouble();
    }

    default IntToDoubleFunction toIntToDoubleFunction() {
        return i -> getAsDouble();
    }

    default DoubleUnaryOperator toDoubleUnaryOperator() {
        return d -> getAsDouble();
    }

    default LongToDoubleFunction toLongToDoubleFunction() {
        return l -> getAsDouble();
    }

}

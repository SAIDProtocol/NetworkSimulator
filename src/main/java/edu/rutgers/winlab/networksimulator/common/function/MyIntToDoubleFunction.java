/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.networksimulator.common.function;

import java.util.Objects;
import java.util.function.DoubleFunction;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;

/**
 *
 * @author jiachen
 */
public interface MyIntToDoubleFunction extends IntToDoubleFunction {

    default <V> IntFunction<V> andThen(DoubleFunction<V> after) {
        Objects.requireNonNull(after);
        return i -> after.apply(applyAsDouble(i));
    }

    default MyIntToDoubleFunction andThen(DoubleUnaryOperator after) {
        Objects.requireNonNull(after);
        return i -> after.applyAsDouble(applyAsDouble(i));
    }

    default IntUnaryOperator andThen(DoubleToIntFunction after) {
        Objects.requireNonNull(after);
        return i -> after.applyAsInt(applyAsDouble(i));
    }

    default IntToLongFunction andThen(DoubleToLongFunction after) {
        Objects.requireNonNull(after);
        return i -> after.applyAsLong(applyAsDouble(i));
    }
}

package org.evrete.api;

import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;

/**
 * A convenient "random object accessor", i.e. function that converts an integer address
 * to an Object. For example, for an array of Objects <code>Object[] arr</code> IntToValue can be expressed
 * as <code>IntToValue itv = i -&gt; arr[i]</code>
 */
@FunctionalInterface
public interface IntToValue extends IntFunction<Object> {

    @SuppressWarnings("unchecked")
    default <Z> Z cast(int value) {
        return (Z) apply(value);
    }

    static IntToValue of(Object[] array) {
        return i -> array[i];
    }

    default IntToValue remap(IntUnaryOperator mapper) {
        return i -> IntToValue.this.apply(mapper.applyAsInt(i));
    }

    default IntToValue remap(final int[] mapping) {
        return value -> IntToValue.this.apply(mapping[value]);
    }
}

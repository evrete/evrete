package org.evrete.api;

import java.util.function.IntFunction;

/**
 * A convenient "random object accessor", i.e. function that converts an integer address
 * to an Object. For example, for an array of Objects <code>Object[] arr</code> IntToValue can be expressed
 * as <code>IntToValue itv = i -&gt; arr[i]</code>
 */
@FunctionalInterface
public interface IntToValue extends IntFunction<Object> {

    static IntToValue of(Object[] array) {
        return i -> array[i];
    }

    default IntToValue remap(final int[] mapping) {
        return value -> IntToValue.this.apply(mapping[value]);
    }
}

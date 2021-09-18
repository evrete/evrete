package org.evrete.api;

import java.util.function.IntFunction;

/**
 * <p>
 * A convenient "random object accessor", i.e. function that converts an integer address
 * to an Object. For example, for an array of Objects {@code Object[] arr} IntToValue
 * can be expressed as {@code IntToValue itv = i -> arr[i]}
 * </p>
 */
@FunctionalInterface
public interface IntToValue extends IntFunction<Object> {

    @SuppressWarnings("unchecked")
    default <T> T get(int i) {
        return (T) apply(i);
    }

}

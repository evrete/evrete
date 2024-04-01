package org.evrete.api;

import java.util.function.IntFunction;

/**
 * Defines an interface for conveniently accessing objects by integer indices. This interface
 * can be used to implement functionality that maps an integer to an object. For instance,
 * in the context of an array of objects {@code Object[] arr}, an implementation of
 * {@code IntToValue} could be represented as {@code IntToValue itv = i -> arr[i];}.
 */
@FunctionalInterface
public interface IntToValue extends IntFunction<Object> {

    /**
     * Retrieves the value at the specified index with the expected return type.
     *
     * @param i the index of the value to retrieve
     * @param <T> the type of the value to retrieve.
     * @return the value at the specified index, cast to the declared type
     */
    @SuppressWarnings("unchecked")
    default <T> T get(int i) {
        return (T) apply(i);
    }

}

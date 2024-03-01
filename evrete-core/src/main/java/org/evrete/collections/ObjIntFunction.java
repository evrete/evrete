package org.evrete.collections;

/**
 * Represents a function that accepts an integer and an object of type T, and produces a result of type R.
 *
 * @param <T> The type of the object parameter.
 * @param <R> The type of the result.
 */
@FunctionalInterface
public interface ObjIntFunction<T, R> {
    R apply(int i, T t);
}

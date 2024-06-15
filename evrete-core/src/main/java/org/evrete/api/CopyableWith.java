package org.evrete.api;

/**
 * A generic alternative to Cloneable that accepts an argument.
 *
 * @param <T> The type parameter of the object being copied.
 * @param <A> The type of the argument accepted by the copy method.
 */
public interface CopyableWith<T, A> {
    /**
     * Creates a copy of an object of type T, potentially modified based on the argument of type A.
     *
     * @param arg The argument that influences the copying process.
     * @return A new instance of type T.
     */
    T copy(A arg);
}

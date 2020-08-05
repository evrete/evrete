package org.evrete.api;

/**
 * <p>
 * A generic alternative to Cloneable
 * </p>
 *
 * @param <T> type parameter
 */
public interface Copyable<T> {
    T copyOf();
}

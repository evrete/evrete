package org.evrete.api;

import java.util.Iterator;

/**
 * The {@code ReIterator} interface represents a reusable {@link Iterator}.
 * <p>
 * Implementing this interface allows an {@link Iterator} to be reset to its initial position.
 * </p>
 *
 * @param <T> the type of elements returned by this iterator
 */
public interface ReIterator<T> extends Iterator<T> {

    /**
     * <p>
     * Resets the iterator to its initial position.
     * </p>
     * <p>
     * Additionally, this method returns the size of the underlying data collection.
     * </p>
     *
     * @return The size of the underlying data collection.
     */
    long reset();
}

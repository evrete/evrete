package org.evrete.api;

import java.util.Iterator;

public interface ReIterator<T> extends Iterator<T> {
    /**
     * Resets the iterator to its initial position
     *
     * @return size of the underlying data
     */
    long reset();
}

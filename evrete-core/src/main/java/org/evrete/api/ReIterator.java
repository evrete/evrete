package org.evrete.api;

import java.util.Iterator;

public interface ReIterator<T> extends Iterator<T> {
    /**
     * Resets the iterator to its initial position
     *
     * @return size of the underlying data
     */
    long reset();

    //TODO implement and use this method in RHS delete() and update()
    // calls to avoid re-iteration over deleted facts.
    default int markDeleted() {
        throw new UnsupportedOperationException("Not implemented");
    }
}

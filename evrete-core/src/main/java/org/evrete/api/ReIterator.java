package org.evrete.api;

import java.util.Iterator;
import java.util.NoSuchElementException;

public interface ReIterator<T> extends Iterator<T> {
    static <Z> ReIterator<Z> emptyIterator() {
        return new ReIterator<Z>() {
            @Override
            public long reset() {
                return 0L;
            }

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Z next() {
                throw new NoSuchElementException();
            }
        };
    }

    /**
     * <p>
     * Resets the iterator to its initial position and returns the size of the underlying data collection
     * </p>
     *
     * @return size of the underlying data
     */
    long reset();
}

package org.evrete.api;

import java.util.Iterator;
import java.util.NoSuchElementException;

public interface ReIterator<T> extends Iterator<T> {
    /**
     * Resets the iterator to its initial position
     *
     * @return size of the underlying data
     */
    long reset();

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
}

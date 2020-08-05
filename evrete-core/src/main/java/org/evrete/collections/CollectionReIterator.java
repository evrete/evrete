package org.evrete.collections;

import org.evrete.api.ReIterator;

import java.util.Collection;
import java.util.Iterator;

public class CollectionReIterator<T> implements ReIterator<T> {
    private final Collection<? extends T> collection;
    private Iterator<? extends T> delegate;

    public CollectionReIterator(Collection<? extends T> collection) {
        this.collection = collection;
        reset();
    }

    @Override
    public long reset() {
        this.delegate = collection.iterator();
        return collection.size();
    }


    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public T next() {
        return delegate.next();
    }
}

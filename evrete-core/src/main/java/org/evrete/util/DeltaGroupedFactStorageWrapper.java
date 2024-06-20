package org.evrete.util;

import org.evrete.api.annotations.NonNull;
import org.evrete.api.spi.DeltaGroupedFactStorage;
import org.evrete.api.spi.MemoryScope;

import java.util.Iterator;

public class DeltaGroupedFactStorageWrapper<V> implements DeltaGroupedFactStorage<V> {
    private final DeltaGroupedFactStorage<V> delegate;

    public DeltaGroupedFactStorageWrapper(DeltaGroupedFactStorage<V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void insert(long key, @NonNull V value) {
        delegate.insert(key, value);
    }

    @Override
    public void delete(long key, @NonNull V value) {
        delegate.delete(key, value);
    }

    @Override
    public Iterator<V> valueIterator(MemoryScope scope, long key) {
        return delegate.valueIterator(scope, key);
    }

    @Override
    public Iterator<Long> iterator(MemoryScope scope) {
        return delegate.iterator(scope);
    }

    @Override
    public void commit() {
        delegate.commit();
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public String toString() {
        return "{" +
                "memory=" + delegate +
                '}';
    }

}

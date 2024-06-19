package org.evrete.util;

import org.evrete.api.annotations.NonNull;
import org.evrete.api.spi.DeltaGroupedFactStorage;
import org.evrete.api.spi.MemoryScope;

import java.util.Iterator;

public class DeltaGroupedFactStorageWrapper<K, V> implements DeltaGroupedFactStorage<K, V> {
    private final DeltaGroupedFactStorage<K, V> delegate;

    public DeltaGroupedFactStorageWrapper(DeltaGroupedFactStorage<K, V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void insert(@NonNull K key, @NonNull V value) {
        delegate.insert(key, value);
    }

    @Override
    public void delete(@NonNull K key, @NonNull V value) {
        delegate.delete(key, value);
    }

    @Override
    public Iterator<V> valueIterator(MemoryScope scope, K key) {
        return delegate.valueIterator(scope, key);
    }

    @Override
    public Iterator<K> iterator(MemoryScope scope) {
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

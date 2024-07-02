package org.evrete.util;

import org.evrete.api.FactHandle;
import org.evrete.api.annotations.Nullable;
import org.evrete.api.spi.FactStorage;

import java.util.Map;
import java.util.stream.Stream;

public class FactStorageWrapper<FH extends FactHandle, V> implements FactStorage<FH, V> {
    private final FactStorage<FH, V> delegate;

    public FactStorageWrapper(FactStorage<FH, V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void insert(FH factHandle, V value) {
        delegate.insert(factHandle, value);
    }

    @Override
    public V remove(FH factHandle) {
        return delegate.remove(factHandle);
    }

    @Override
    @Nullable
    public V get(FH factHandle) {
        return delegate.get(factHandle);
    }

    @Override
    public Stream<Map.Entry<FH, V>> stream() {
        return delegate.stream();
    }

    @Override
    public void clear() {
        delegate.clear();
    }
}

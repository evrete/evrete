package org.evrete.spi.minimal;

import org.evrete.api.FactHandle;
import org.evrete.api.spi.FactStorage;
import org.evrete.collections.LongKeyMap;
import org.evrete.util.MapEntryImpl;

import java.util.Map;
import java.util.stream.Stream;

public class DefaultFactStorage<FH extends FactHandle, V> implements FactStorage<FH, V> {
    private final Storage<FH, V> delegate = new Storage<>();

    @Override
    public void insert(FH factHandle, V value) {
        delegate.insert(factHandle, value);
    }

    @Override
    public V remove(FH factHandle) {
        return delegate.remove(factHandle);
    }

    @Override
    public V get(FH factHandle) {
        return delegate.get(factHandle);
    }

    @Override
    public void clear() {
        this.delegate.clear();
    }

    @Override
    public Stream<Map.Entry<FH, V>> stream() {
        return delegate.storage.values().map(fhvMapEntry -> fhvMapEntry);
    }

    private static class Storage<FH extends FactHandle, V>  {
        //TODO size config option
        private final LongKeyMap<MapEntryImpl<FH, V>> storage = new LongKeyMap<>();

        synchronized void insert(FH factHandle, V value) {
            storage.put(factHandle.getId(), new MapEntryImpl<>(factHandle, value));
        }

        synchronized V remove(FH factHandle) {
            MapEntryImpl<FH, V> found = storage.remove(factHandle.getId());
            return found == null ? null : found.getValue();
        }

        V get(FH factHandle) {
            MapEntryImpl<FH, V> found = storage.get(factHandle.getId());
            return found == null ? null : found.getValue();
        }

        synchronized void clear() {
            this.storage.clear();
        }
    }
}

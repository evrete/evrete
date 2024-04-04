package org.evrete.spi.minimal;

import org.evrete.api.*;
import org.evrete.util.CollectionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

abstract class AbstractKeyedFactStorage<K extends MemoryKey, T extends AbstractFactsMap<K>> implements KeyedFactStorage {
    private final T[] maps;
    private MemoryKeyHashed currentRecord = null;

    AbstractKeyedFactStorage(Class<T> mapType, Function<KeyMode, T> mapSupplier) {
        //this.fields = fields;
        this.maps = CollectionUtils.array(mapType, KeyMode.values().length);
        for (KeyMode mode : KeyMode.values()) {
            this.maps[mode.ordinal()] = mapSupplier.apply(mode);
        }
    }

    abstract MemoryKeyHashed writeKey(FieldValue h);

    @Override
    public final void commitChanges() {
        T main = get(KeyMode.OLD_OLD);
        main.merge(get(KeyMode.NEW_NEW));
        main.merge(get(KeyMode.OLD_NEW));
    }


    @Override
    public final ReIterator<FactHandleVersioned> values(KeyMode mode, MemoryKey key) {
        return get(mode).values(key);
    }

    @Override
    public final void clear() {
        for (T map : maps) {
            map.clear();
        }
    }

    private void insert(MemoryKeyHashed key, Collection<FactHandleVersioned> factHandles) {
        if (get(KeyMode.OLD_OLD).hasKey(key)) {
            // Existing key
            get(KeyMode.OLD_NEW).add(key, factHandles);
        } else {
            // New key
            get(KeyMode.NEW_NEW).add(key, factHandles);
        }
    }

    @Override
    public void write(FieldValue partialKey) {
        this.currentRecord = writeKey(partialKey);
    }

    @Override
    public void write(Collection<FactHandleVersioned> factHandles) {
        insert(currentRecord, factHandles);
        this.currentRecord = null;
    }

    public final ReIterator<MemoryKey> keys(KeyMode keyMode) {
        return get(keyMode).keys();
    }

    final T get(KeyMode mode) {
        return maps[mode.ordinal()];
    }

    @Override
    public String toString() {
        return Arrays.toString(maps);
    }

}

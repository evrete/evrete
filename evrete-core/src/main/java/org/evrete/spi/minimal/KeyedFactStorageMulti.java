package org.evrete.spi.minimal;

import org.evrete.api.*;

class KeyedFactStorageMulti extends AbstractKeyedFactStorage<FactsMapMulti> {

    KeyedFactStorageMulti(int initialSize, ActiveField[] fields) {
        super(FactsMapMulti.class, mode -> new FactsMapMulti(fields, mode, initialSize));
    }

    @Override
    public ReIterator<MemoryKey> keys(KeyMode keyMode) {
        return get(keyMode).keys();
    }

    @Override
    public ReIterator<FactHandleVersioned> values(KeyMode mode, MemoryKey key) {
        return get(mode).values((MemoryKeyMulti) key);
    }

    @Override
    public void commitChanges() {
        FactsMapMulti main = get(KeyMode.MAIN);
        main.merge(get(KeyMode.UNKNOWN_UNKNOWN));
        main.merge(get(KeyMode.KNOWN_UNKNOWN));
    }

    @Override
    public void insert(FieldToValueHandle key, int keyHash, FactHandleVersioned value) {
        if (get(KeyMode.MAIN).hasKey(keyHash, key)) {
            // Existing key
            get(KeyMode.KNOWN_UNKNOWN).add(key, keyHash, value);
        } else {
            // New key
            get(KeyMode.UNKNOWN_UNKNOWN).add(key, keyHash, value);
        }
    }
}

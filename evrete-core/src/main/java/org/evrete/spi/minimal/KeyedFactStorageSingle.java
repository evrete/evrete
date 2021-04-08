package org.evrete.spi.minimal;

import org.evrete.api.*;

class KeyedFactStorageSingle extends AbstractKeyedFactStorage<FactsMapSingle> {

    KeyedFactStorageSingle(int initialSize, ActiveField field) {
        super(FactsMapSingle.class, mode -> new FactsMapSingle(field, mode, initialSize));
    }

    @Override
    public ReIterator<MemoryKey> keys(KeyMode keyMode) {
        return get(keyMode).keys();
    }

    @Override
    public ReIterator<FactHandleVersioned> values(KeyMode mode, MemoryKey key) {
        MemoryKeySingle k = (MemoryKeySingle) key;
        return get(mode).values(k);
    }

    @Override
    public void commitChanges() {
        FactsMapSingle main = get(KeyMode.MAIN);
        main.merge(get(KeyMode.UNKNOWN_UNKNOWN));
        main.merge(get(KeyMode.KNOWN_UNKNOWN));
    }

    @Override
    public void insert(FieldToValueHandle key, int keyHash, FactHandleVersioned value) {
        if (get(KeyMode.MAIN).hasKey(key, keyHash)) {
            // Existing key
            get(KeyMode.KNOWN_UNKNOWN).add(key, keyHash, value);
        } else {
            // New key
            get(KeyMode.UNKNOWN_UNKNOWN).add(key, keyHash, value);
        }
    }
}

package org.evrete.spi.minimal;

import org.evrete.api.*;

class SharedBetaData extends AbstractBetaFactStorage<FieldsFactMap> {

    SharedBetaData(int initialSize, ActiveField[] fields) {
        super(FieldsFactMap.class, mode -> new FieldsFactMap(fields, mode, initialSize));
    }

    @Override
    public ReIterator<MemoryKey> keys(KeyMode keyMode) {
        return get(keyMode).keys();
    }

    @Override
    public ReIterator<FactHandleVersioned> values(KeyMode mode, MemoryKey key) {
        return get(mode).values((MemoryKeyImpl) key);
    }

    @Override
    public void commitChanges() {
        FieldsFactMap main = get(KeyMode.MAIN);
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

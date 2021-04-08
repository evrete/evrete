package org.evrete.spi.minimal;

import org.evrete.api.*;

class SharedBetaDataPlain extends AbstractBetaFactStorage<FieldsFactMapPlain> {
    private final ActiveField field;

    SharedBetaDataPlain(int initialSize, ActiveField field) {
        super(FieldsFactMapPlain.class, mode -> new FieldsFactMapPlain(mode, initialSize));
        this.field = field;
    }

    @Override
    public ReIterator<MemoryKey> keys(KeyMode keyMode) {
        return get(keyMode).keys();
    }

    @Override
    public ReIterator<FactHandleVersioned> values(KeyMode mode, MemoryKey key) {
        MemoryKeyImplPlain k = (MemoryKeyImplPlain) key;
        return get(mode).values(k);
    }

    @Override
    public void commitChanges() {
        FieldsFactMapPlain main = get(KeyMode.MAIN);
        main.merge(get(KeyMode.UNKNOWN_UNKNOWN));
        main.merge(get(KeyMode.KNOWN_UNKNOWN));
    }

    @Override
    public void insert(FieldToValueHandle key, int keyHash, FactHandleVersioned value) {
        ValueHandle h = key.apply(field);
        int hash = keyHash;
        MemoryKeyImplPlain memoryKey = new MemoryKeyImplPlain(h, hash);

        if (get(KeyMode.MAIN).hasKey(memoryKey)) {
            // Existing key
            get(KeyMode.KNOWN_UNKNOWN).add(memoryKey, value);
        } else {
            // New key
            get(KeyMode.UNKNOWN_UNKNOWN).add(memoryKey, value);
        }
    }
}

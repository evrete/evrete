package org.evrete.spi.minimal;

import org.evrete.api.*;

class SharedBetaDataPlain implements SharedBetaFactStorage {
    private final ActiveField field;
    private final FieldsFactMapPlain[] maps = new FieldsFactMapPlain[KeyMode.values().length];

    SharedBetaDataPlain(ActiveField field) {
        this.field = field;
        for (KeyMode mode : KeyMode.values()) {
            //TODO !!!! set in configuration
            this.maps[mode.ordinal()] = new FieldsFactMapPlain(mode, 4096);
        }
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
    public void clear() {
        for (FieldsFactMapPlain map : maps) {
            map.clear();
        }
    }

    @Override
    public void commitChanges() {
        FieldsFactMapPlain main = get(KeyMode.MAIN);
        main.merge(get(KeyMode.UNKNOWN_UNKNOWN));
        main.merge(get(KeyMode.KNOWN_UNKNOWN));
    }

    @Override
    public void insert(FieldToValueHandle key, FactHandleVersioned value) {
        ValueHandle h = key.apply(field);
        int hash = h.hashCode();
        MemoryKeyImplPlain memoryKey = new MemoryKeyImplPlain(h, hash);

        if (get(KeyMode.MAIN).hasKey(memoryKey)) {
            // Existing key
            get(KeyMode.KNOWN_UNKNOWN).add(memoryKey, value);
        } else {
            // New key
            get(KeyMode.UNKNOWN_UNKNOWN).add(memoryKey, value);
        }
    }

    private FieldsFactMapPlain get(KeyMode mode) {
        return maps[mode.ordinal()];
    }
}

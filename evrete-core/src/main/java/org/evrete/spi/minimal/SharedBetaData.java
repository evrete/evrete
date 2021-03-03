package org.evrete.spi.minimal;

import org.evrete.api.*;

import java.util.Objects;

class SharedBetaData implements SharedBetaFactStorage {
    private final ActiveField[] fields;
    private final FieldsFactMap[] maps = new FieldsFactMap[KeyMode.values().length];

    SharedBetaData(ActiveField[] fields) {
        this.fields = fields;
        for (KeyMode mode : KeyMode.values()) {
            this.maps[mode.ordinal()] = new FieldsFactMap(mode);
        }
    }

    @Override
    public ReIterator<MemoryKey> keys(KeyMode keyMode) {
        return get(keyMode).keys();
    }

    @Override
    public ReIterator<FactHandleVersioned> values(KeyMode mode, FieldToValueHandle key) {
        MemoryKeyImpl k = (MemoryKeyImpl) key;
        return get(mode).values(k);
    }

    @Override
    public void clear() {
        for (FieldsFactMap map : maps) {
            map.clear();
        }
    }

    private int hash(FieldToValueHandle key) {
        int hash = 0;
        for (ActiveField field : fields) {
            hash ^= Objects.hashCode(key.apply(field));
        }
        return hash;
    }

    @Override
    public void commitChanges() {
        FieldsFactMap main = get(KeyMode.MAIN);
        main.merge(get(KeyMode.UNKNOWN_UNKNOWN));
        main.merge(get(KeyMode.KNOWN_UNKNOWN));
    }

    @Override
    public void insert(FieldToValueHandle key, FactHandleVersioned value) {
        int hash = hash(key);
        ValueHandle[] data = new ValueHandle[fields.length];
        for (int i = 0; i < fields.length; i++) {
            ActiveField field = fields[i];
            data[i] = key.apply(field);
        }
        MemoryKeyImpl memoryKey = new MemoryKeyImpl(data, hash);

        if (get(KeyMode.MAIN).hasKey(memoryKey)) {
            // Existing key
            get(KeyMode.KNOWN_UNKNOWN).add(memoryKey, value);
        } else {
            // New key
            get(KeyMode.UNKNOWN_UNKNOWN).add(memoryKey, value);
        }
    }

    private FieldsFactMap get(KeyMode mode) {
        return maps[mode.ordinal()];
    }
}

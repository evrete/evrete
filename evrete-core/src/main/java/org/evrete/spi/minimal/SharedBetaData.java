package org.evrete.spi.minimal;

import org.evrete.api.*;

import java.util.Objects;

class SharedBetaData extends AbstractBetaFactStorage<FieldsFactMap> {
    private final ActiveField[] fields;

    SharedBetaData(int initialSize, ActiveField[] fields) {
        super(FieldsFactMap.class, mode -> new FieldsFactMap(mode, initialSize));
        this.fields = fields;
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

    private int hash(FieldToValueHandle key) {
        int hash = 0;
        for (ActiveField field : fields) {
            hash ^= Objects.hashCode(key.apply(field));
        }
        return hash;
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
}

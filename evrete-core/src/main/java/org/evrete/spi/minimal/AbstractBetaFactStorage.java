package org.evrete.spi.minimal;

import org.evrete.api.KeyMode;
import org.evrete.api.SharedBetaFactStorage;
import org.evrete.util.CollectionUtils;

import java.util.function.Function;

abstract class AbstractBetaFactStorage<T extends AbstractFieldsFactMap> implements SharedBetaFactStorage {
    private final T[] maps;// = new FieldsFactMap[KeyMode.values().length];

    AbstractBetaFactStorage(Class<T> mapType, Function<KeyMode, T> mapSupplier) {
        //this.fields = fields;
        this.maps = CollectionUtils.array(mapType, KeyMode.values().length);
        for (KeyMode mode : KeyMode.values()) {
            this.maps[mode.ordinal()] = mapSupplier.apply(mode);
        }
    }

/*
    @Override
    public ReIterator<MemoryKey> keys(KeyMode keyMode) {
        return get(keyMode).keys();
    }

    @Override
    public ReIterator<FactHandleVersioned> values(KeyMode mode, MemoryKey key) {
        return get(mode).values((MemoryKeyImpl) key);
    }
*/

    @Override
    public final void clear() {
        for (T map : maps) {
            map.clear();
        }
    }


/*
    @Override
    public final void commitChanges() {
        T main = get(KeyMode.MAIN);
        main.merge(get(KeyMode.UNKNOWN_UNKNOWN));
        main.merge(get(KeyMode.KNOWN_UNKNOWN));
    }
*/

/*
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
*/

    final T get(KeyMode mode) {
        return maps[mode.ordinal()];
    }
}

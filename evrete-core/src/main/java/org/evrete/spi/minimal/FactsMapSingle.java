package org.evrete.spi.minimal;

import org.evrete.api.*;

import java.util.Objects;

class FactsMapSingle extends AbstractFactsMap<MemoryKeySingle> {
    private final ActiveField field;

    FactsMapSingle(ActiveField field, KeyMode myMode, int minCapacity) {
        super(myMode, minCapacity);
        this.field = field;
    }

    @Override
    MemoryKeySingle newKeyInstance(IntToValueHandle fieldValues, int hash) {
        return new MemoryKeySingle(fieldValues.apply(0), hash);
    }

    @Override
    MemoryKeySingle newKeyInstance(FieldToValueHandle fieldValues, int hash) {
        return new MemoryKeySingle(fieldValues.apply(field), hash);
    }

    @Override
    boolean sameData(MapKey<MemoryKeySingle> mapEntry, FieldToValueHandle fieldToValueHandle) {
        ValueHandle h1 = mapEntry.key.data;
        ValueHandle h2 = fieldToValueHandle.apply(field);
        return Objects.equals(h1, h2);
    }

    @Override
    boolean sameData1(MapKey<MemoryKeySingle> mapEntry, IntToValueHandle key) {
        ValueHandle h1 = mapEntry.key.data;
        ValueHandle h2 = key.apply(0);
        return Objects.equals(h1, h2);
    }

}

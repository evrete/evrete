package org.evrete.spi.minimal;

import org.evrete.api.IntToValueHandle;
import org.evrete.api.KeyMode;

import java.util.Objects;

class FactsMapMulti extends AbstractFactsMap<MemoryKeyMulti> {
    private final int fieldCount;

    FactsMapMulti(int fieldCount, KeyMode myMode, int minCapacity) {
        super(myMode, minCapacity);
        this.fieldCount = fieldCount;
    }

    @Override
    boolean sameData(MapKey<MemoryKeyMulti> mapEntry, IntToValueHandle key) {
        for (int i = 0; i < fieldCount; i++) {
            if (!Objects.equals(mapEntry.key.get(i), key.apply(i))) return false;
        }
        return true;
    }

    @Override
    MemoryKeyMulti newKeyInstance(IntToValueHandle fieldValues, int hash) {
        return new MemoryKeyMulti(fieldCount, fieldValues, hash);
    }
}

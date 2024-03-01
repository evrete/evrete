package org.evrete.spi.minimal;

import java.util.Objects;

class FactsMapMulti extends AbstractFactsMap<MemoryKeyMulti> {
    private final int fieldCount;

    FactsMapMulti(int fieldCount) {
        this.fieldCount = fieldCount;
    }

    @Override
    boolean sameData(FactsWithKey<MemoryKeyMulti> factsWithKey, IntToValueHandle key) {
        for (int i = 0; i < fieldCount; i++) {
            if (!Objects.equals(factsWithKey.key.get(i), key.apply(i))) return false;
        }
        return true;
    }

    @Override
    MemoryKeyMulti newKeyInstance(MemoryKeyHashed key) {
        return new MemoryKeyMulti(fieldCount, key);
    }
}

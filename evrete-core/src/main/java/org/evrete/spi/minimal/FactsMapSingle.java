package org.evrete.spi.minimal;

import org.evrete.api.IntToValueHandle;
import org.evrete.api.KeyMode;
import org.evrete.api.ValueHandle;

import java.util.Objects;

class FactsMapSingle extends AbstractFactsMap<MemoryKeySingle> {

    FactsMapSingle(KeyMode myMode, int minCapacity) {
        super(myMode, minCapacity);
    }

    @Override
    MemoryKeySingle newKeyInstance(IntToValueHandle fieldValues, int hash) {
        return new MemoryKeySingle(fieldValues.apply(0), hash);
    }

    @Override
    boolean sameData(MapKey<MemoryKeySingle> mapEntry, IntToValueHandle key) {
        ValueHandle h1 = mapEntry.key.data;
        ValueHandle h2 = key.apply(0);
        return Objects.equals(h1, h2);
    }

}

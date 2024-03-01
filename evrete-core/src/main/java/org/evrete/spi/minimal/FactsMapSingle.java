package org.evrete.spi.minimal;

import org.evrete.api.ValueHandle;

import java.util.Objects;

class FactsMapSingle extends AbstractFactsMap<MemoryKeySingle> {

    @Override
    MemoryKeySingle newKeyInstance(MemoryKeyHashed key) {
        return new MemoryKeySingle(key);
    }

    @Override
    boolean sameData(FactsWithKey<MemoryKeySingle> factsWithKey, IntToValueHandle key) {
        ValueHandle h1 = factsWithKey.key.data;
        ValueHandle h2 = key.apply(0);
        return Objects.equals(h1, h2);
    }

}

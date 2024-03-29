package org.evrete.spi.minimal;

import org.evrete.api.ValueHandle;

class KeyedFactStorageSingle extends AbstractKeyedFactStorage<MemoryKeySingle, FactsMapSingle> {

    private final SingleState state = new SingleState();

    KeyedFactStorageSingle() {
        super(FactsMapSingle.class, mode -> new FactsMapSingle());
    }

    @Override
    MemoryKeyHashed writeKey(ValueHandle h) {
        this.state.values = value -> h;
        this.state.hash = h.hashCode();
        return this.state;
    }

    private static class SingleState extends MemoryKeyHashed {

    }
}

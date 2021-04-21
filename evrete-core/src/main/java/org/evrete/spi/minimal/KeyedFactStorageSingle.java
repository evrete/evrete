package org.evrete.spi.minimal;

import org.evrete.api.KeyMode;
import org.evrete.api.ValueHandle;

class KeyedFactStorageSingle extends AbstractKeyedFactStorage<FactsMapSingle> {

    private final SingleState state = new SingleState();

    KeyedFactStorageSingle(int initialSize) {
        super(FactsMapSingle.class, mode -> new FactsMapSingle(mode, initialSize));
    }

    @Override
    KeyState writeKey(ValueHandle h) {
        this.state.values = value -> h;
        this.state.hash = h.hashCode();
        return this.state;
    }

    @Override
    public void commitChanges() {
        FactsMapSingle main = get(KeyMode.MAIN);
        main.merge(get(KeyMode.UNKNOWN_UNKNOWN));
        main.merge(get(KeyMode.KNOWN_UNKNOWN));
    }

    private static class SingleState extends AbstractKeyedFactStorage.KeyState {

    }
}

package org.evrete.spi.minimal;

import org.evrete.api.ActiveField;
import org.evrete.api.KeyMode;

class KeyedFactStorageSingle extends AbstractKeyedFactStorage<FactsMapSingle> {

    KeyedFactStorageSingle(int initialSize, ActiveField field) {
        super(FactsMapSingle.class, mode -> new FactsMapSingle(field, mode, initialSize));
    }

    @Override
    public void commitChanges() {
        FactsMapSingle main = get(KeyMode.MAIN);
        main.merge(get(KeyMode.UNKNOWN_UNKNOWN));
        main.merge(get(KeyMode.KNOWN_UNKNOWN));
    }
}

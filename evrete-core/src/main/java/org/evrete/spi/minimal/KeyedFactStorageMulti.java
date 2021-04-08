package org.evrete.spi.minimal;

import org.evrete.api.ActiveField;
import org.evrete.api.KeyMode;

class KeyedFactStorageMulti extends AbstractKeyedFactStorage<FactsMapMulti> {

    KeyedFactStorageMulti(int initialSize, ActiveField[] fields) {
        super(FactsMapMulti.class, mode -> new FactsMapMulti(fields, mode, initialSize));
    }

    @Override
    public void commitChanges() {
        FactsMapMulti main = get(KeyMode.MAIN);
        main.merge(get(KeyMode.UNKNOWN_UNKNOWN));
        main.merge(get(KeyMode.KNOWN_UNKNOWN));
    }
}

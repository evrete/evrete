package org.evrete.spi.minimal;

import org.evrete.api.FieldToValueHandle;
import org.evrete.api.KeyMode;
import org.evrete.api.MemoryKey;

import java.util.function.BiPredicate;

abstract class AbstractFactsMap<K extends MemoryKey, E extends AbstractFactsMap.MapKey<K>> {
    final int myModeOrdinal;
    final BiPredicate<E, FieldToValueHandle> search;

    AbstractFactsMap(KeyMode myMode) {
        this.search = this::sameData;
        this.myModeOrdinal = myMode.ordinal();
    }

    abstract void clear();

    abstract boolean sameData(E mapEntry, FieldToValueHandle key);

    static class MapKey<K> {
        final LinkedFactHandles facts = new LinkedFactHandles();
        final K key;

        MapKey(K key) {
            this.key = key;
        }
    }
}

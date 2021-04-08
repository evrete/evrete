package org.evrete.spi.minimal;

import org.evrete.api.FieldToValueHandle;
import org.evrete.api.KeyMode;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;
import org.evrete.collections.LinearHashSet;

import java.util.function.BiPredicate;
import java.util.function.Function;

abstract class AbstractFactsMap<K extends MemoryKey, E extends AbstractFactsMap.MapKey<K>> {
    final BiPredicate<E, K> SEARCH_PREDICATE = (entry, memoryKey) -> entry.key.equals(memoryKey);
    final LinearHashSet<E> data;
    final int myModeOrdinal;
    final BiPredicate<E, FieldToValueHandle> search;
    private final Function<E, MemoryKey> ENTRY_MAPPER = entry -> entry.key;

    AbstractFactsMap(KeyMode myMode, int minCapacity) {
        this.search = this::sameData;
        this.myModeOrdinal = myMode.ordinal();
        this.data = new LinearHashSet<>(minCapacity);
    }

    abstract void clear();

    abstract boolean sameData(E mapEntry, FieldToValueHandle key);

    final ReIterator<MemoryKey> keys() {
        return data.iterator(ENTRY_MAPPER);
    }

    final boolean hasKey(int hash, FieldToValueHandle key) {
        int addr = data.findBinIndex(key, hash, search);
        return data.get(addr) != null;
    }

    static class MapKey<K> {
        final LinkedFactHandles facts = new LinkedFactHandles();
        final K key;

        MapKey(K key) {
            this.key = key;
        }
    }
}

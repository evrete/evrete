package org.evrete.spi.minimal;

import org.evrete.api.*;
import org.evrete.collections.LinearHashSet;

import java.util.function.BiPredicate;
import java.util.function.Function;

abstract class AbstractFactsMap<K extends MemoryKey, E extends AbstractFactsMap.MapKey<K>> {
    private final BiPredicate<E, K> SEARCH_PREDICATE = (entry, memoryKey) -> entry.key.equals(memoryKey);
    final LinearHashSet<E> data;
    final int myModeOrdinal;
    final BiPredicate<E, FieldToValueHandle> search;
    private final Function<E, MemoryKey> ENTRY_MAPPER = entry -> entry.key;

    AbstractFactsMap(KeyMode myMode, int minCapacity) {
        this.search = this::sameData;
        this.myModeOrdinal = myMode.ordinal();
        this.data = new LinearHashSet<>(minCapacity);
    }

    abstract boolean sameData(E mapEntry, FieldToValueHandle key);

    final ReIterator<MemoryKey> keys() {
        return data.iterator(ENTRY_MAPPER);
    }

    final boolean hasKey(int hash, FieldToValueHandle key) {
        int addr = data.findBinIndex(key, hash, search);
        return data.get(addr) != null;
    }

    final int addr(K key) {
        return data.findBinIndex(key, key.hashCode(), SEARCH_PREDICATE);
    }

    final ReIterator<FactHandleVersioned> values(K key) {
        // TODO !!!! analyze usage, return null and call remove() on the corresponding key iterator
        int addr = addr(key);
        E entry = data.get(addr);
        return entry == null ? ReIterator.emptyIterator() : entry.facts.iterator();
    }

    public final void clear() {
        data.clear();
    }

    @Override
    public final String toString() {
        return data.toString();
    }

    static class MapKey<K> {
        final LinkedFactHandles facts = new LinkedFactHandles();
        final K key;

        MapKey(K key) {
            this.key = key;
        }
    }
}

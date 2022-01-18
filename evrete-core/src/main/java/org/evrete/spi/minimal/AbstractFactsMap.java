package org.evrete.spi.minimal;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.IntToValueHandle;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;
import org.evrete.collections.LinearHashSet;
import org.evrete.util.Constants;

import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.function.Function;

abstract class AbstractFactsMap<K extends MemoryKey> {
    private static final ReIterator<FactHandleVersioned> EMPTY = ReIterator.emptyIterator();
    private final LinearHashSet<MapKey<K>> data;
    private final BiPredicate<MapKey<K>, IntToValueHandle> search;
    private final BiPredicate<MapKey<K>, K> SEARCH_PREDICATE = (entry, memoryKey) -> entry.key.equals(memoryKey);
    private final Function<MapKey<K>, MemoryKey> ENTRY_MAPPER = entry -> entry.key;

    AbstractFactsMap(int minCapacity) {
        this.search = this::sameData;
        this.data = new LinearHashSet<>(minCapacity);
    }

    abstract boolean sameData(MapKey<K> mapEntry, IntToValueHandle key);

    abstract K newKeyInstance(IntToValueHandle fieldValues, int hash);

    final ReIterator<MemoryKey> keys() {
        return data.iterator(ENTRY_MAPPER);
    }

    public final void add(IntToValueHandle key, int keyHash, Collection<FactHandleVersioned> factHandles) {
        data.resize();
        int pos = data.findBinIndex(key, keyHash, search);
        MapKey<K> entry = data.get(pos);
        if (entry == null) {
            K k = newKeyInstance(key, keyHash);
            entry = new MapKey<>(k);
            // TODO saveDirect is doing unnecessary job
            data.saveDirect(entry, pos);
        }
        for (FactHandleVersioned h : factHandles) {
            entry.facts.add(h);
        }

    }

    final boolean hasKey(int hash, IntToValueHandle key) {
        int pos = data.findBinIndex(key, hash, search);
        return data.get(pos) != null;
    }

    private int address(K key) {
        return data.findBinIndex(key, key.hashCode(), SEARCH_PREDICATE);
    }

    @SuppressWarnings("unchecked")
    final ReIterator<FactHandleVersioned> values(MemoryKey k) {
        int pos = address((K) k);
        MapKey<K> entry = data.get(pos);
        return entry == null ? EMPTY : entry.facts.iterator();
    }

    // TODO implement merge
    final void merge(AbstractFactsMap<K> other) {
        other.data.forEachDataEntry(this::merge);
        other.data.clear();
    }

    private void merge(MapKey<K> otherEntry) {
        this.data.resize();

        int hash = otherEntry.hashCode();
        this.data.apply(hash, dataKey -> {
            //return dataKey.key.equals(otherEntry.key);
            return dataKey.isDeleted() || dataKey.key.equals(otherEntry.key);
        }, (found, pos) -> {
            if (found == null || found.isDeleted()) {
                data.saveDirect(otherEntry, pos);
            } else {
                found.facts.consume(otherEntry.facts);
            }
        });
    }

    public final void clear() {
        data.clear();
    }

    @Override
    public final String toString() {
        return data.toString();
    }

    static class MapKey<K extends MemoryKey> {
        final LinkedFactHandles facts = new LinkedFactHandles();
        final K key;

        MapKey(K key) {
            this.key = key;
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MapKey<?> mapKey = (MapKey<?>) o;
            return this.key.equals(mapKey.key);
        }

        boolean isDeleted() {
            return key.getMetaValue() == Constants.DELETED_MEMORY_KEY_FLAG;
        }

        @Override
        public final int hashCode() {
            return key.hashCode();
        }

        @Override
        public String toString() {
            return key + "={" + facts + '}';
        }
    }
}

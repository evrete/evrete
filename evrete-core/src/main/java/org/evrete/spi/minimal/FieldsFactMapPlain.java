package org.evrete.spi.minimal;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.KeyMode;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;
import org.evrete.collections.LinearHashSet;

import java.util.function.BiPredicate;
import java.util.function.Function;

class FieldsFactMapPlain {
    private static final BiPredicate<MapEntry, MemoryKeyImplPlain> SEARCH_PREDICATE = (entry, memoryKey) -> entry.key.equals(memoryKey);
    private static final Function<MapEntry, MemoryKey> ENTRY_MAPPER = entry -> entry.key;
    private final int myModeOrdinal;
    private final LinearHashSet<MapEntry> data;

    FieldsFactMapPlain(KeyMode myMode, int minCapacity) {
        this.myModeOrdinal = myMode.ordinal();
        this.data = new LinearHashSet<>(minCapacity);
    }

    public void clear() {
        data.clear();
    }

    void merge(FieldsFactMapPlain other) {
        other.data.forEachDataEntry(this::merge);
        other.data.clear();
    }

    private void merge(MapEntry otherEntry) {
        otherEntry.key.setMetaValue(myModeOrdinal);
        int addr = addr(otherEntry.key);
        MapEntry found = data.get(addr);
        if (found == null) {
            this.data.add(otherEntry);
        } else {
            found.facts.consume(otherEntry.facts);
        }
    }

    ReIterator<MemoryKey> keys() {
        return data.iterator(ENTRY_MAPPER);
    }

    ReIterator<FactHandleVersioned> values(MemoryKeyImplPlain key) {
        // TODO !!!! analyze usage, return null and call remove() on the corresponding key iterator
        int addr = addr(key);
        MapEntry entry = data.get(addr);
        return entry == null ? ReIterator.emptyIterator() : entry.facts.iterator();
    }

    public void add(MemoryKeyImplPlain key, FactHandleVersioned factHandleVersioned) {
        key.setMetaValue(myModeOrdinal);

        data.resize();
        int addr = addr(key);
        MapEntry entry = data.get(addr);
        if (entry == null) {
            entry = new MapEntry(key);
            // TODO saveDirect is doing unnecessary job
            data.saveDirect(entry, addr);
        }
        entry.facts.add(factHandleVersioned);
    }

    boolean hasKey(MemoryKeyImplPlain key) {
        int addr = addr(key);
        MapEntry entry = data.get(addr);
        return entry != null;
    }

    private int addr(MemoryKeyImplPlain key) {
        return data.findBinIndex(key, key.hashCode(), SEARCH_PREDICATE);
    }

    @Override
    public String toString() {
        return data.toString();
    }

    private static class MapEntry {
        final LinkedFactHandles facts = new LinkedFactHandles();
        private final MemoryKeyImplPlain key;

        MapEntry(MemoryKeyImplPlain key) {
            this.key = key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MapEntry mapEntry = (MapEntry) o;
            return key.equals(mapEntry.key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }
}

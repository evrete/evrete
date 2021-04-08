package org.evrete.spi.minimal;

import org.evrete.api.*;
import org.evrete.collections.LinearHashSet;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

class FieldsFactMap extends AbstractFieldsFactMap {
    private static final BiPredicate<MapEntry, MemoryKeyImpl> SEARCH_PREDICATE = (entry, memoryKey) -> entry.key.equals(memoryKey);
    private final int myModeOrdinal;
    private static final Function<MapEntry, MemoryKey> ENTRY_MAPPER = entry -> entry.key;
    private final LinearHashSet<MapEntry> data;
    private final ActiveField[] fields;
    private final BiPredicate<MapEntry, FieldToValueHandle> search;

    FieldsFactMap(ActiveField[] fields, KeyMode myMode, int minCapacity) {
        this.fields = fields;
        this.myModeOrdinal = myMode.ordinal();
        this.data = new LinearHashSet<>(minCapacity);
        this.search = this::sameData;
    }

    public void clear() {
        data.clear();
    }

    void merge(FieldsFactMap other) {
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

    ReIterator<FactHandleVersioned> values(MemoryKeyImpl key) {
        // TODO !!!! analyze usage, return null and call remove() on the corresponding key iterator
        int addr = addr(key);
        MapEntry entry = data.get(addr);
        return entry == null ? ReIterator.emptyIterator() : entry.facts.iterator();
    }

    public void add(FieldToValueHandle key, int hash, FactHandleVersioned factHandleVersioned) {
        data.resize();
        int addr = data.findBinIndex(key, hash, search);
        MapEntry entry = data.get(addr);
        if (entry == null) {
            MemoryKeyImpl k = new MemoryKeyImpl(fields, key, hash);
            k.setMetaValue(myModeOrdinal);
            entry = new MapEntry(k);
            // TODO saveDirect is doing unnecessary job
            data.saveDirect(entry, addr);
        }
        entry.facts.add(factHandleVersioned);
    }

    boolean hasKey(int hash, FieldToValueHandle key) {
        int addr = data.findBinIndex(key, hash, search);
        return data.get(addr) != null;
    }


    private boolean sameData(MapEntry mapEntry, FieldToValueHandle fieldToValueHandle) {
        for (int i = 0; i < fields.length; i++) {
            ValueHandle h1 = mapEntry.key.get(i);
            ValueHandle h2 = fieldToValueHandle.apply(fields[i]);
            if (!Objects.equals(h1, h2)) return false;
        }
        return true;
    }

    private int addr(MemoryKeyImpl key) {
        return data.findBinIndex(key, key.hashCode(), SEARCH_PREDICATE);
    }

    @Override
    public String toString() {
        return data.toString();
    }

    private static class MapEntry {
        final LinkedFactHandles facts = new LinkedFactHandles();
        private final MemoryKeyImpl key;


        MapEntry(MemoryKeyImpl key) {
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

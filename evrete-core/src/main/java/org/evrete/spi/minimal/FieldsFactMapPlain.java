package org.evrete.spi.minimal;

import org.evrete.api.*;
import org.evrete.collections.LinearHashSet;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

class FieldsFactMapPlain extends AbstractFieldsFactMap {
    private static final BiPredicate<MapEntry, MemoryKeyImplPlain> SEARCH_PREDICATE = (entry, memoryKey) -> entry.key.equals(memoryKey);
    private static final Function<MapEntry, MemoryKey> ENTRY_MAPPER = entry -> entry.key;
    private final int myModeOrdinal;
    private final LinearHashSet<MapEntry> data;
    private final ActiveField field;
    private final BiPredicate<MapEntry, FieldToValueHandle> search;


    FieldsFactMapPlain(ActiveField field, KeyMode myMode, int minCapacity) {
        this.myModeOrdinal = myMode.ordinal();
        this.field = field;
        this.data = new LinearHashSet<>(minCapacity);
        this.search = this::sameData;
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

    public void add(FieldToValueHandle key, int keyHash, FactHandleVersioned factHandleVersioned) {
        data.resize();
        int addr = data.findBinIndex(key, keyHash, search);
        MapEntry entry = data.get(addr);
        if (entry == null) {
            MemoryKeyImplPlain k = new MemoryKeyImplPlain(key.apply(field), keyHash);
            k.setMetaValue(myModeOrdinal);
            entry = new MapEntry(k);
            // TODO saveDirect is doing unnecessary job
            data.saveDirect(entry, addr);
        }
        entry.facts.add(factHandleVersioned);

    }

    boolean hasKey(FieldToValueHandle key, int hash) {
        int addr = data.findBinIndex(key, hash, search);
        return data.get(addr) != null;
    }

    private boolean sameData(MapEntry mapEntry, FieldToValueHandle fieldToValueHandle) {
        ValueHandle h1 = mapEntry.key.data;
        ValueHandle h2 = fieldToValueHandle.apply(field);
        return Objects.equals(h1, h2);
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
        final MemoryKeyImplPlain key;

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

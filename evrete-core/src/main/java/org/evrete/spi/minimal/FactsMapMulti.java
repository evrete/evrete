package org.evrete.spi.minimal;

import org.evrete.api.*;

import java.util.Objects;

class FactsMapMulti extends AbstractFactsMap<MemoryKeyMulti, FactsMapMulti.MapEntryMulti> {
    private final ActiveField[] fields;

    FactsMapMulti(ActiveField[] fields, KeyMode myMode, int minCapacity) {
        super(myMode, minCapacity);
        this.fields = fields;
    }

    public void clear() {
        data.clear();
    }

    void merge(FactsMapMulti other) {
        other.data.forEachDataEntry(this::merge);
        other.data.clear();
    }

    private void merge(MapEntryMulti otherEntry) {
        otherEntry.key.setMetaValue(myModeOrdinal);
        int addr = addr(otherEntry.key);
        MapEntryMulti found = data.get(addr);
        if (found == null) {
            this.data.add(otherEntry);
        } else {
            found.facts.consume(otherEntry.facts);
        }
    }

    ReIterator<FactHandleVersioned> values(MemoryKeyMulti key) {
        // TODO !!!! analyze usage, return null and call remove() on the corresponding key iterator
        int addr = addr(key);
        MapEntryMulti entry = data.get(addr);
        return entry == null ? ReIterator.emptyIterator() : entry.facts.iterator();
    }

    public void add(FieldToValueHandle key, int hash, FactHandleVersioned factHandleVersioned) {
        data.resize();
        int addr = data.findBinIndex(key, hash, search);
        MapEntryMulti entry = data.get(addr);
        if (entry == null) {
            MemoryKeyMulti k = new MemoryKeyMulti(fields, key, hash);
            k.setMetaValue(myModeOrdinal);
            entry = new MapEntryMulti(k);
            // TODO saveDirect is doing unnecessary job
            data.saveDirect(entry, addr);
        }
        entry.facts.add(factHandleVersioned);
    }

    @Override
    boolean sameData(MapEntryMulti mapEntry, FieldToValueHandle fieldToValueHandle) {
        for (int i = 0; i < fields.length; i++) {
            ValueHandle h1 = mapEntry.key.get(i);
            ValueHandle h2 = fieldToValueHandle.apply(fields[i]);
            if (!Objects.equals(h1, h2)) return false;
        }
        return true;
    }

    int addr(MemoryKeyMulti key) {
        return data.findBinIndex(key, key.hashCode(), SEARCH_PREDICATE);
    }

    @Override
    public String toString() {
        return data.toString();
    }

    static class MapEntryMulti extends AbstractFactsMap.MapKey<MemoryKeyMulti> {

        MapEntryMulti(MemoryKeyMulti key) {
            super(key);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MapEntryMulti mapEntry = (MapEntryMulti) o;
            return key.equals(mapEntry.key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }
}

package org.evrete.spi.minimal;

import org.evrete.api.*;

import java.util.Objects;

class FactsMapSingle extends AbstractFactsMap<MemoryKeySingle, FactsMapSingle.MapEntrySingle> {
    private final ActiveField field;

    FactsMapSingle(ActiveField field, KeyMode myMode, int minCapacity) {
        super(myMode, minCapacity);
        this.field = field;
    }

    public void clear() {
        data.clear();
    }

    void merge(FactsMapSingle other) {
        other.data.forEachDataEntry(this::merge);
        other.data.clear();
    }

    private void merge(MapEntrySingle otherEntry) {
        otherEntry.key.setMetaValue(myModeOrdinal);
        int addr = addr(otherEntry.key);
        MapEntrySingle found = data.get(addr);
        if (found == null) {
            this.data.add(otherEntry);
        } else {
            found.facts.consume(otherEntry.facts);
        }
    }

    ReIterator<FactHandleVersioned> values(MemoryKeySingle key) {
        // TODO !!!! analyze usage, return null and call remove() on the corresponding key iterator
        int addr = addr(key);
        MapEntrySingle entry = data.get(addr);
        return entry == null ? ReIterator.emptyIterator() : entry.facts.iterator();
    }

    public void add(FieldToValueHandle key, int keyHash, FactHandleVersioned factHandleVersioned) {
        data.resize();
        int addr = data.findBinIndex(key, keyHash, search);
        MapEntrySingle entry = data.get(addr);
        if (entry == null) {
            MemoryKeySingle k = new MemoryKeySingle(key.apply(field), keyHash);
            k.setMetaValue(myModeOrdinal);
            entry = new MapEntrySingle(k);
            // TODO saveDirect is doing unnecessary job
            data.saveDirect(entry, addr);
        }
        entry.facts.add(factHandleVersioned);

    }

    @Override
    boolean sameData(MapEntrySingle mapEntry, FieldToValueHandle fieldToValueHandle) {
        ValueHandle h1 = mapEntry.key.data;
        ValueHandle h2 = fieldToValueHandle.apply(field);
        return Objects.equals(h1, h2);
    }


    int addr(MemoryKeySingle key) {
        return data.findBinIndex(key, key.hashCode(), SEARCH_PREDICATE);
    }

    @Override
    public String toString() {
        return data.toString();
    }

    static class MapEntrySingle extends AbstractFactsMap.MapKey<MemoryKeySingle> {

        MapEntrySingle(MemoryKeySingle key) {
            super(key);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MapEntrySingle mapEntry = (MapEntrySingle) o;
            return key.equals(mapEntry.key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }
}

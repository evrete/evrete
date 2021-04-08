package org.evrete.spi.minimal;

abstract class AbstractFactsMap {

    abstract void clear();

/*
    private static final BiPredicate<MapEntry, MemoryKeyImpl> SEARCH_PREDICATE = (entry, memoryKey) -> entry.key.equals(memoryKey);
    private final int myModeOrdinal;
    private static final Function<MapEntry, MemoryKey> ENTRY_MAPPER = entry -> entry.key;
    private final LinearHashSet<MapEntry> data;

    AbstractFieldsFactMap(KeyMode myMode, int minCapacity) {
        this.myModeOrdinal = myMode.ordinal();
        this.data = new LinearHashSet<>(minCapacity);
    }

    public void clear() {
        data.clear();
    }

    void merge(AbstractFieldsFactMap other) {
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

    public void add(MemoryKeyImpl key, FactHandleVersioned factHandleVersioned) {
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

    boolean hasKey(MemoryKeyImpl key) {
        int addr = addr(key);
        MapEntry entry = data.get(addr);
        return entry != null;
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
*/
}

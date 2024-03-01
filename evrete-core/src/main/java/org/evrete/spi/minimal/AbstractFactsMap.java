package org.evrete.spi.minimal;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;
import org.evrete.collections.LinearHashSet;
import org.evrete.util.Constants;

import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.function.Function;

abstract class AbstractFactsMap<K extends MemoryKey> {
    private static final ReIterator<FactHandleVersioned> EMPTY = ReIterator.emptyIterator();
    private final LinearHashSet<FactsWithKey<K>> data;
    private final BiPredicate<FactsWithKey<K>, MemoryKeyHashed> search;
    private final BiPredicate<FactsWithKey<K>, K> SEARCH_PREDICATE = (entry, memoryKey) -> entry.key.equals(memoryKey);
    private final Function<FactsWithKey<K>, MemoryKey> ENTRY_MAPPER = entry -> entry.key;

    AbstractFactsMap() {
        this.search = (key, memoryKey) -> sameData(key, memoryKey.values);
        this.data = new LinearHashSet<>();
    }

    abstract boolean sameData(FactsWithKey<K> mapEntry, IntToValueHandle key);

    abstract K newKeyInstance(MemoryKeyHashed key);


    final ReIterator<MemoryKey> keys() {
        return data.iterator(ENTRY_MAPPER);
    }

    public void add(MemoryKeyHashed key, Collection<FactHandleVersioned> factHandles) {
        FactsWithKey<K> entry = data.computeIfAbsent(key, this.search, k -> new FactsWithKey<>(newKeyInstance(k)));
        for (FactHandleVersioned h : factHandles) {
            entry.facts.add(h);
        }
    }

    final boolean hasKey(MemoryKeyHashed key) {
        return data.contains(key, search);
    }

    @SuppressWarnings("unchecked")
    final ReIterator<FactHandleVersioned> values(MemoryKey k) {
        FactsWithKey<K> entry = data.get((K) k, SEARCH_PREDICATE);
        return entry == null ? EMPTY : entry.facts.iterator();
    }

    final void merge(AbstractFactsMap<K> other) {
        merge(other.data);
    }

    final void merge(LinearHashSet<FactsWithKey<K>> data) {
        this.data.addAll(data, (local, external) -> {
            if (local == null || local.isDeleted()) {
                return external;
            } else {
                local.facts.consume(external.facts);
                return local;
            }
        });

        data.clear();
    }

    public final void clear() {
        data.clear();
    }

    @Override
    public final String toString() {
        return data.toString();
    }

    static class FactsWithKey<K extends MemoryKey> {
        final LinkedFactHandles facts = new LinkedFactHandles();
        final K key;

        FactsWithKey(K key) {
            this.key = key;
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FactsWithKey<?> factsWithKey = (FactsWithKey<?>) o;
            return this.key.equals(factsWithKey.key);
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

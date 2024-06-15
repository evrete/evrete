package org.evrete.runtime.rete;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

class HashedCollection {
    private Set<ConditionMemory.MemoryEntry> data = new HashSet<>();

    void reset() {
        this.data = new HashSet<>();
    }

    void delete(Predicate<ConditionMemory.MemoryEntry> predicate) {
        // Actual logic of the method
        if (this.data.removeIf(predicate)) {
            this.data = new HashSet<>(this.data);
        }
    }

    Iterator<ConditionMemory.MemoryEntry> iterator() {
        return data.iterator();
    }

    int size() {
        return data.size();
    }

    void add(ConditionMemory.MemoryEntry entry) {
        this.data.add(entry);
    }
}

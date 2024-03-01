package org.evrete.spi.minimal;

import org.evrete.api.MemoryKey;
import org.evrete.api.MemoryKeyCollection;
import org.evrete.api.ReIterator;
import org.evrete.api.annotations.NonNull;
import org.evrete.collections.LinkedDataRW;

class DefaultMemoryKeyCollection implements MemoryKeyCollection {
    private final LinkedDataRW<MemoryKey> data = new LinkedDataRW<>();

    DefaultMemoryKeyCollection() {

    }

    @NonNull
    @Override
    public ReIterator<MemoryKey> iterator() {
        return data.iterator();
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public void add(MemoryKey key) {
        this.data.add(key);
    }

    @Override
    public String toString() {
        return data.toString();
    }
}

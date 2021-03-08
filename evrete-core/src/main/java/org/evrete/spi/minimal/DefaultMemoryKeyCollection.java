package org.evrete.spi.minimal;

import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;
import org.evrete.collections.LinkedDataRW;
import org.evrete.runtime.MemoryKeyCollection;

class DefaultMemoryKeyCollection implements MemoryKeyCollection {
    private final LinkedDataRW<MemoryKey> data = new LinkedDataRW<>();

    DefaultMemoryKeyCollection() {

    }

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

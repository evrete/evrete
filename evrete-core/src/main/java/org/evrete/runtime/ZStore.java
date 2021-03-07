package org.evrete.runtime;

import org.evrete.api.IntToMemoryKey;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;
import org.evrete.collections.CollectionReIterator;

import java.util.LinkedList;

class ZStore implements ZStoreI {
    private final FactType[] mapping;
    private final LinkedList<MemoryKey[]> data = new LinkedList<>();

    ZStore(FactType[] mapping) {
        this.mapping = mapping;
    }

    @Override
    public ReIterator<MemoryKey[]> entries() {
        return new CollectionReIterator<>(data);
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public void append(ZStoreI other) {
        this.data.addAll(((ZStore) other).data);
    }

    @Override
    public void save(IntToMemoryKey key) {
        MemoryKey[] k = new MemoryKey[mapping.length];
        for (int i = 0; i < mapping.length; i++) {
            k[i] = key.apply(i);
        }
        this.data.add(k);
    }

    @Override
    public String toString() {
        return data.toString();
    }
}

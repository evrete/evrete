package org.evrete.spi.minimal;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.FieldToValueHandle;
import org.evrete.api.ReIterator;
import org.evrete.api.SharedPlainFactStorage;
import org.evrete.collections.CollectionReIterator;

import java.util.LinkedList;
import java.util.List;

public class SharedAlphaData implements SharedPlainFactStorage {
    private final List<FactHandleVersioned> data = new LinkedList<>();

    @Override
    public void insert(FactHandleVersioned fact, FieldToValueHandle key) {
        data.add(fact);
    }

    @Override
    public void insert(SharedPlainFactStorage other) {
        int size = other.size();
        if (size == 0) return;
        if (other instanceof SharedAlphaData) {
            SharedAlphaData sad = (SharedAlphaData) other;
            this.data.addAll(sad.data);
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public ReIterator<FactHandleVersioned> iterator() {
        return new CollectionReIterator<>(data);
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public void commitChanges() {
    }

    @Override
    public String toString() {
        return data.toString();
    }
}

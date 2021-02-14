package org.evrete.spi.minimal;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.ReIterator;
import org.evrete.api.SharedPlainFactStorage;
import org.evrete.collections.CollectionReIterator;

import java.util.ArrayList;
import java.util.List;

public class SharedAlphaData implements SharedPlainFactStorage {
    private final List<FactHandleVersioned> data = new ArrayList<>();

    @Override
    public void insert(FactHandleVersioned fact) {
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
            this.ensureExtraCapacity(size);
            other.iterator().forEachRemaining(this::insert);
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
}

package org.evrete.runtime.memory;

import org.evrete.api.IntToValueRow;
import org.evrete.api.KeysStore;
import org.evrete.api.ReIterator;

import java.util.function.IntFunction;
import java.util.function.Predicate;

class KeysStoreStub implements KeysStore {

    @Override
    public void save(IntFunction<IntToValueRow> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void append(KeysStore other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <P extends Predicate<IntToValueRow>> void delete(P[] predicates, int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Entry get(IntToValueRow key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ReIterator<Entry> entries() {
        throw new UnsupportedOperationException("Not implemented in " + getClass().getName());
    }
}

package org.evrete.util;

import org.evrete.api.IntToMemoryKey;
import org.evrete.api.KeysStore;
import org.evrete.api.ReIterator;

import java.util.function.IntFunction;

public class KeysStoreStub implements KeysStore {

    @Override
    public void save(IntFunction<IntToMemoryKey> values) {
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
    public ReIterator<Entry> entries() {
        throw new UnsupportedOperationException("Not implemented in " + getClass().getName());
    }
}

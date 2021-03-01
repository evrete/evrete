package org.evrete.api;

import java.util.function.IntFunction;

public interface KeysStore {

    void clear();

    void save(IntFunction<IntToValueRow> values);

    void append(KeysStore other);

    ReIterator<Entry> entries();

    interface Entry {
        MemoryKey[] key();

        KeysStore getNext();
    }
}

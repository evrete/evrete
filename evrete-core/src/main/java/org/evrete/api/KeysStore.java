package org.evrete.api;

import java.util.function.IntFunction;

public interface KeysStore {

    void clear();

    void save(IntFunction<IntToValueRow> values);

    Entry get(IntToValueRow key);

    default boolean hasKey(IntToValueRow key) {
        return get(key) != null;
    }

    void append(KeysStore other);

    ReIterator<Entry> entries();

    interface Entry {
        ValueRow[] key();

        KeysStore getNext();
    }
}

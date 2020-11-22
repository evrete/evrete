package org.evrete.api;

import java.util.function.IntFunction;
import java.util.function.Predicate;

public interface KeysStore {

    boolean isEmpty();

    void clear();

    void save(IntFunction<IntToValueRow> values);

    Entry get(IntToValueRow key);

    default boolean hasKey(IntToValueRow key) {
        return get(key) != null;
    }

    void append(KeysStore other);

    ReIterator<Entry> entries();

    @Deprecated
    default <P extends Predicate<IntToValueRow>> void delete(P[] predicates) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Deprecated
    default <P extends Predicate<IntToValueRow>> void delete(P[] predicates, int index) {
        throw new UnsupportedOperationException("Deprecated");
    }

    interface Entry {
        ValueRow[] key();

        KeysStore getNext();
    }
}

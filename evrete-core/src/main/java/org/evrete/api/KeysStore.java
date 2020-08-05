package org.evrete.api;

import java.util.function.IntFunction;
import java.util.function.Predicate;

public interface KeysStore {

    default boolean isEmpty() {
        return keyCount() == 0;
    }

    void clear();

    long keyCount();

    void save(IntFunction<IntToValueRow> values);

    KeysStore getNext(IntToValueRow key);

    void append(KeysStore other);

    ReIterator<Entry> entries();

    default <P extends Predicate<IntToValueRow>> void delete(P[] predicates) {
        delete(predicates, 0);
    }

    <P extends Predicate<IntToValueRow>> void delete(P[] predicates, int index);

    interface Entry {
        ValueRow[] key();

        KeysStore getNext();
    }
}

package org.evrete.spi.minimal;

import org.evrete.api.IntToValueRow;
import org.evrete.api.KeysStore;
import org.evrete.api.ReIterator;
import org.evrete.api.ValueRow;
import org.evrete.collections.CollectionReIterator;
import org.evrete.collections.MappedReIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

class KeysStorePlain implements KeysStore {
    private final ArrayList<ValueRow[]> collection;
    private final int level;
    private final int arrSize;
    private final ReIterator<ValueRow[]> reIterator;

    KeysStorePlain(int level, int arrSize) {
        this.collection = new ArrayList<>();
        this.level = level;
        this.arrSize = arrSize;
        this.reIterator = new CollectionReIterator<>(collection);
    }

    @Override
    public void clear() {
        collection.clear();
    }

    @Override
    public ReIterator<Entry> entries() {
        DummyEntry entry = new DummyEntry();
        return new MappedReIterator<>(reIterator, entry);
    }

    @Override
    public long keyCount() {
        return collection.size();
    }

    @Override
    public void save(IntFunction<IntToValueRow> values) {
        collection.add(MiscUtils.toArray(values.apply(level), arrSize));
    }

    @Override
    public KeysStore getNext(IntToValueRow key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void append(KeysStore store) {
        KeysStorePlain other = (KeysStorePlain) store;
        this.collection.addAll(other.collection);
    }

    @Override
    public final String toString() {
        StringJoiner j = new StringJoiner(", ");
        for (ValueRow[] arr : collection) {
            j.add(Arrays.toString(arr));
        }
        return j.toString();
    }

    @Override
    public final <P extends Predicate<IntToValueRow>> void delete(P[] predicates, int index) {
        Predicate<IntToValueRow> p = predicates[index];
        collection.removeIf(values -> {
            IntToValueRow iv = i -> values[i];
            return p.test(iv);
        });
    }


    private static class DummyEntry implements KeysStore.Entry, Function<ValueRow[], Entry> {
        ValueRow[] key;

        @Override
        public ValueRow[] key() {
            return key;
        }

        @Override
        public Entry apply(ValueRow[] valueRows) {
            this.key = valueRows;
            return this;
        }

        @Override
        public KeysStore getNext() {
            throw new UnsupportedOperationException();
        }
    }

}

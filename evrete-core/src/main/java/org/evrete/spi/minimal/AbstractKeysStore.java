package org.evrete.spi.minimal;

import org.evrete.api.KeysStore;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;
import org.evrete.collections.AbstractLinearHash;

import java.util.Arrays;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntFunction;

abstract class AbstractKeysStore<E extends KeysStoreEntry> extends AbstractLinearHash<E> implements KeysStore {
    static final BiPredicate<KeysStoreEntry, MemoryKey[]> EQ_FUNCTION = (entry, rows) -> Arrays.equals(entry.key, rows);
    private static final BiPredicate<Object, Object> EQ_PREDICATE = (o1, o2) -> {
        KeysStoreEntry e1 = (KeysStoreEntry) o1;
        KeysStoreEntry e2 = (KeysStoreEntry) o2;
        return e1.eq(e2);
    };
    private static final ToIntFunction<Object> HASH_FUNCTION = value -> ((KeysStoreEntry) value).hash;
    private static final Function<KeysStoreEntry, Entry> KEY_MAPPER = entry -> entry;
    final int arrSize;
    final int level;

    AbstractKeysStore(int minCapacity, int level, int arrSize) {
        super(minCapacity);
        this.level = level;
        this.arrSize = arrSize;
    }

    @Override
    protected final ToIntFunction<Object> getHashFunction() {
        return HASH_FUNCTION;
    }


    @Override
    public final ReIterator<Entry> entries() {
        return iterator(KEY_MAPPER);
    }

    @Override
    protected final BiPredicate<Object, Object> getEqualsPredicate() {
        return EQ_PREDICATE;
    }


}

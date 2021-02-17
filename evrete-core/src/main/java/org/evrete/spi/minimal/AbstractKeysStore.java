package org.evrete.spi.minimal;

import org.evrete.api.IntToValueRow;
import org.evrete.api.KeysStore;
import org.evrete.api.ReIterator;
import org.evrete.api.ValueRow;
import org.evrete.collections.AbstractLinearHash;

import java.util.Arrays;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntFunction;

abstract class AbstractKeysStore<E extends KeysStoreEntry> extends AbstractLinearHash<E> implements KeysStore {
    static final BiPredicate<Object, Object> EQ_PREDICATE = (o1, o2) -> {
        KeysStoreEntry e1 = (KeysStoreEntry) o1;
        KeysStoreEntry e2 = (KeysStoreEntry) o2;
        return e1.eq(e2);
    };

    static final ToIntFunction<Object> HASH_FUNCTION = value -> ((KeysStoreEntry) value).hash;
    //static final BiPredicate<KeysStoreEntry, ValueRow[]> EQ_FUNCTION_OLD = (entry, rows) -> MiscUtils.sameData1(entry.key, rows);
    static final BiPredicate<KeysStoreEntry, ValueRow[]> EQ_FUNCTION = (entry, rows) -> Arrays.equals(entry.key, rows);
    private static final BiPredicate<KeysStoreEntry, IntToValueRow> EQ_FUNCTION1 = (entry, intToValueRow) -> MiscUtils.eqEquals(intToValueRow, entry.key);
    private static final Function<KeysStoreEntry, Entry> KEY_MAPPER = entry -> entry;
    final int arrSize;
    final int level;

    public AbstractKeysStore(int level, int arrSize) {
        this.level = level;
        this.arrSize = arrSize;
    }

    @Override
    protected final ToIntFunction<Object> getHashFunction() {
        return HASH_FUNCTION;
    }

    @Override
    public final Entry get(IntToValueRow key) {
        int hash = MiscUtils.hash(key, arrSize);
        return get(findBinIndex(key, hash, EQ_FUNCTION1));
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

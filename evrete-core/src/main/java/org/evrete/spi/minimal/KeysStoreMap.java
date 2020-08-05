package org.evrete.spi.minimal;

import org.evrete.api.IntToValueRow;
import org.evrete.api.KeysStore;
import org.evrete.api.ReIterator;
import org.evrete.api.ValueRow;
import org.evrete.collections.AbstractHashData;

import java.util.Arrays;
import java.util.function.*;

class KeysStoreMap extends AbstractHashData<KeysStoreMap.MapEntry> implements KeysStore {
    private static final BiPredicate<KeysStoreMap.MapEntry, ValueRow[]> EQ_FUNCTION = (entry, rows) -> MiscUtils.sameData(entry.key, rows);
    private static final BiPredicate<MapEntry, IntToValueRow> EQ_FUNCTION1 = (entry, intToValueRow) -> MiscUtils.eqIdentity(intToValueRow, entry.key);
    private static final BiPredicate<Object, Object> EQ_PREDICATE = (o1, o2) -> {
        MapEntry e1 = (MapEntry) o1;
        MapEntry e2 = (MapEntry) o2;
        return e1.eq(e2);
    };
    private static final ToIntFunction<Object> HASH_FUNCTION = value -> ((MapEntry) value).hash;
    protected final int level;
    protected final Supplier<KeysStore> storeSupplier;
    private final int arrSize;

    private static final Function<MapEntry, Entry> KEY_MAPPER = entry -> entry;

    protected KeysStoreMap(int level, int arrSize, Supplier<KeysStore> storeSupplier) {
        super(16);
        this.level = level;
        this.arrSize = arrSize;
        this.storeSupplier = storeSupplier;
    }

    @Override
    public KeysStore getNext(IntToValueRow key) {
        int hash = MiscUtils.hash(key, arrSize);
        MapEntry e = get(findBinIndex(key, hash, EQ_FUNCTION1));
        return e == null ? null : e.next;
    }

    @Override
    public ReIterator<Entry> entries() {
        return iterator(KEY_MAPPER);
    }

    @Override
    public long keyCount() {
        return size();
    }

    @Override
    protected final ToIntFunction<Object> getHashFunction() {
        return HASH_FUNCTION;
    }

    @Override
    protected final BiPredicate<Object, Object> getEqualsPredicate() {
        return EQ_PREDICATE;
    }

    @Override
    public final <P extends Predicate<IntToValueRow>> void delete(P[] predicates, int index) {
        int i, idx;
        MapEntry entry;

        for (i = 0; i < currentInsertIndex; i++) {
            idx = getAt(i);
            if ((entry = get(idx)) != null) {
                entry.next.delete(predicates, index + 1);

                ValueRow[] arr = entry.key;
                IntToValueRow iv = z -> arr[z];


                if (predicates[index].test(iv) || entry.next.isEmpty()) {
                    markDeleted(idx);
                }
            }
        }
    }

    @Override
    public final void save(IntFunction<IntToValueRow> values) {
        resize();
        ValueRow[] key = MiscUtils.toArray(values.apply(level), arrSize);
        int hash = MiscUtils.hash(key);
        int addr = findBinIndex(key, hash, EQ_FUNCTION);
        MapEntry found = get(addr);
        if (found == null) {
            found = new KeysStoreMap.MapEntry(key, hash, storeSupplier.get());
            saveDirect(found, addr);
        }
        found.next.save(values);
    }

    @Override
    public final void append(KeysStore other) {
        this.bulkAdd((KeysStoreMap) other);
    }

    final static class MapEntry implements KeysStore.Entry {
        final ValueRow[] key;
        final KeysStore next;
        final int hash;

        MapEntry(ValueRow[] key, int hash, KeysStore next) {
            this.key = key;
            this.next = next;
            this.hash = hash;
        }

        @Override
        public ValueRow[] key() {
            return key;
        }

        @Override
        public final KeysStore getNext() {
            return next;
        }

        boolean eq(MapEntry other) {
            return MiscUtils.sameData(key, other.key);
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return eq((MapEntry) o);
        }

        @Override
        public final int hashCode() {
            return hash;
        }


        @Override
        public String toString() {
            return Arrays.toString(key) + "->" + next;
        }
    }
}

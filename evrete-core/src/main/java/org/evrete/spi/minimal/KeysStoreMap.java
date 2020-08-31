package org.evrete.spi.minimal;

import org.evrete.api.IntToValueRow;
import org.evrete.api.KeysStore;
import org.evrete.api.ValueRow;

import java.util.Arrays;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

class KeysStoreMap extends AbstractKeysStore<KeysStoreMap.MapEntry> {
    private final Supplier<KeysStore> storeSupplier;

    KeysStoreMap(int level, int arrSize, Supplier<KeysStore> storeSupplier) {
        super(level, arrSize);
        this.storeSupplier = storeSupplier;
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

    final static class MapEntry extends KeysStoreEntry {
        final KeysStore next;

        MapEntry(ValueRow[] key, int hash, KeysStore next) {
            super(key, hash);
            this.next = next;
        }

        @Override
        public final KeysStore getNext() {
            return next;
        }

        @Override
        public String toString() {
            return Arrays.toString(key) + "->" + next;
        }
    }
}

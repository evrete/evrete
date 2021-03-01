package org.evrete.spi.minimal;

import org.evrete.api.IntToValueRow;
import org.evrete.api.KeysStore;
import org.evrete.api.MemoryKey;

import java.util.Arrays;
import java.util.function.IntFunction;
import java.util.function.Supplier;

class KeysStoreMap extends AbstractKeysStore<KeysStoreMap.MapEntry> {
    private final Supplier<KeysStore> storeSupplier;

    KeysStoreMap(int level, int arrSize, Supplier<KeysStore> storeSupplier) {
        super(level, arrSize);
        this.storeSupplier = storeSupplier;
    }

    @Override
    public final void save(IntFunction<IntToValueRow> values) {
        resize();
        MemoryKey[] key = MiscUtils.toArray(values.apply(level), arrSize);
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

        MapEntry(MemoryKey[] key, int hash, KeysStore next) {
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

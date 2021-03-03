package org.evrete.spi.minimal;

import org.evrete.api.IntToValueRow;
import org.evrete.api.KeysStore;
import org.evrete.api.MemoryKey;

import java.util.Arrays;
import java.util.StringJoiner;
import java.util.function.IntFunction;

class KeysStorePlain extends AbstractKeysStore<KeysStorePlain.MapEntry> {

    KeysStorePlain(int minCapacity, int level, int arrSize) {
        super(minCapacity, level, arrSize);
    }

    @Override
    public void save(IntFunction<IntToValueRow> values) {
        resize();
        MemoryKey[] key = MiscUtils.toArray(values.apply(level), arrSize);
        int hash = MiscUtils.hash(key);
        int addr = findBinIndex(key, hash, EQ_FUNCTION);
        KeysStorePlain.MapEntry found = get(addr);
        if (found == null) {
            found = new KeysStorePlain.MapEntry(key, hash);
            saveDirect(found, addr);
        }
    }

    @Override
    public final void append(KeysStore store) {
        KeysStorePlain other = (KeysStorePlain) store;
        super.bulkAdd(other);
    }

    @Override
    public final String toString() {
        StringJoiner j = new StringJoiner(", ");
        forEachDataEntry(arr -> j.add(Arrays.toString(arr.key)));
        return j.toString();
    }


    final static class MapEntry extends KeysStoreEntry {

        MapEntry(MemoryKey[] key, int hash) {
            super(key, hash);
        }

        @Override
        public final KeysStore getNext() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return Arrays.toString(key);
        }
    }

}

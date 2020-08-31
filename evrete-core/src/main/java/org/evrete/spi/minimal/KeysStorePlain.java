package org.evrete.spi.minimal;

import org.evrete.api.IntToValueRow;
import org.evrete.api.KeysStore;
import org.evrete.api.ValueRow;

import java.util.Arrays;
import java.util.StringJoiner;
import java.util.function.IntFunction;
import java.util.function.Predicate;

class KeysStorePlain extends AbstractKeysStore<KeysStorePlain.MapEntry> {

    KeysStorePlain(int level, int arrSize) {
        super(level, arrSize);
    }

    @Override
    public void save(IntFunction<IntToValueRow> values) {
        resize();
        ValueRow[] key = MiscUtils.toArray(values.apply(level), arrSize);
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

    @Override
    public final <P extends Predicate<IntToValueRow>> void delete(P[] predicates, int index) {
        for (int i = 0; i < currentInsertIndex; i++) {
            int idx = getAt(i);
            MapEntry entry;
            if ((entry = get(idx)) != null) {
                IntToValueRow iv = z -> entry.key[z];
                if (predicates[index].test(iv)) {
                    markDeleted(idx);
                }
            }
        }
    }

    final static class MapEntry extends KeysStoreEntry {

        MapEntry(ValueRow[] key, int hash) {
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

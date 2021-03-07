package org.evrete.spi.minimal;

import org.evrete.api.IntToMemoryKey;
import org.evrete.api.KeysStore;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;
import org.evrete.collections.CollectionReIterator;
import org.evrete.collections.MappedReIterator;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.function.IntFunction;

class KeysStorePlain implements KeysStore {
    private final LinkedList<KeysStorePlain.MapEntry> data = new LinkedList<>();
    private final int level, arrSize;

    KeysStorePlain(int level, int arrSize) {
        //super(minCapacity, level, arrSize);
        this.level = level;
        this.arrSize = arrSize;
    }

    @Override
    public void clear() {
        this.data.clear();
    }

    @Override
    public void save(IntFunction<IntToMemoryKey> values) {
        MemoryKey[] key = MiscUtils.toArray(values.apply(level), arrSize);
        KeysStorePlain.MapEntry entry = new MapEntry(key);
        this.data.add(entry);
    }

    @Override
    public void append(KeysStore other) {
        KeysStorePlain o = (KeysStorePlain) other;
        this.data.addAll(o.data);
    }

    @Override
    public ReIterator<Entry> entries() {
        ReIterator<MapEntry> it = new CollectionReIterator<>(this.data);
        return new MappedReIterator<>(it, mapEntry -> mapEntry);
    }


    /*
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
*/


    final static class MapEntry implements KeysStore.Entry {
        final MemoryKey[] key;

        MapEntry(MemoryKey[] key) {
            this.key = key;
        }

        public MemoryKey[] key() {
            return key;
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

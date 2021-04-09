package org.evrete.spi.minimal;

import org.evrete.api.*;

import java.util.Collection;
import java.util.NoSuchElementException;

class SharedAlphaData implements KeyedFactStorage {
    private final LinkedFactHandles[] dataWrappers;
    private final KeyIterator[] keyIterators;

    SharedAlphaData() {
        this.dataWrappers = new LinkedFactHandles[KeyMode.values().length];
        this.keyIterators = new KeyIterator[KeyMode.values().length];
        for (KeyMode mode : KeyMode.values()) {
            int idx = mode.ordinal();
            this.dataWrappers[idx] = new LinkedFactHandles();
            this.keyIterators[idx] = new KeyIterator(mode);
        }
    }

    @Override
    public void insert(FieldToValueHandle key, int keyHash, Collection<FactHandleVersioned> factHandles) {
        LinkedFactHandles data = get(KeyMode.KNOWN_UNKNOWN);
        for (FactHandleVersioned h : factHandles) {
            data.add(h);
        }
    }

    private LinkedFactHandles get(KeyMode mode) {
        return dataWrappers[mode.ordinal()];
    }

    @Override
    public ReIterator<MemoryKey> keys(KeyMode mode) {
        return this.keyIterators[mode.ordinal()];
    }

    @Override
    public ReIterator<FactHandleVersioned> values(KeyMode mode, MemoryKey key) {
        return get(mode).iterator();
    }

    @Override
    public void clear() {
        for (LinkedFactHandles wrapper : this.dataWrappers) {
            wrapper.clear();
        }
    }

    @Override
    public void commitChanges() {
        LinkedFactHandles delta = get(KeyMode.KNOWN_UNKNOWN);
        get(KeyMode.MAIN).consume(delta);
    }

    private static class KeyIterator implements ReIterator<MemoryKey> {
        private static final ValueHandle[] EMPTY = new ValueHandle[0];
        private final MemoryKeyMulti row;
        private boolean hasNext = true;

        KeyIterator(KeyMode mode) {
            this.row = new MemoryKeyMulti(EMPTY, 0);
            this.row.setMetaValue(mode.ordinal());
        }

        @Override
        public long reset() {
            hasNext = true;
            return 1L;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public MemoryKey next() {
            if (hasNext) {
                hasNext = false;
                return row;
            } else {
                throw new NoSuchElementException();
            }
        }
    }
}

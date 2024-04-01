package org.evrete.spi.minimal;

import org.evrete.api.*;

import java.util.Collection;
import java.util.NoSuchElementException;

class SharedAlphaData implements KeyedFactStorage {
    private final LinkedFactHandles[] dataWrappers;

    SharedAlphaData() {
        this.dataWrappers = new LinkedFactHandles[KeyMode.values().length];
        for (KeyMode mode : KeyMode.values()) {
            int idx = mode.ordinal();
            this.dataWrappers[idx] = new LinkedFactHandles();
        }
    }

    @Override
    public void write(FieldValue partialKey) {
        // We have zero keys, this method won't be called
    }

    @Override
    public void write(Collection<FactHandleVersioned> factHandles) {
        LinkedFactHandles data = get(KeyMode.OLD_NEW);
        for (FactHandleVersioned h : factHandles) {
            data.add(h);
        }
    }

    LinkedFactHandles get(KeyMode mode) {
        return dataWrappers[mode.ordinal()];
    }

    @Override
    public ReIterator<MemoryKey> keys(KeyMode mode) {
        return new KeyIterator();
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
        LinkedFactHandles delta = get(KeyMode.OLD_NEW);
        get(KeyMode.OLD_OLD).consume(delta);
    }

    private static class KeyIterator implements ReIterator<MemoryKey> {
        private final MemoryKeyMulti row;
        private boolean hasNext = true;

        KeyIterator() {
            this.row = new MemoryKeyMulti();
        }

        @Override
        public long reset() {
            hasNext = true;
            return 1L;
        }

        @Override
        public void remove() {
            // silently skip
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

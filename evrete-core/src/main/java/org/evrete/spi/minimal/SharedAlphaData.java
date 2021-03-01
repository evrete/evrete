package org.evrete.spi.minimal;

import org.evrete.api.*;
import org.evrete.collections.CollectionReIterator;

import java.util.LinkedList;
import java.util.NoSuchElementException;

class SharedAlphaData implements SharedBetaFactStorage {
    private final DataWrapper[] dataWrappers;
    private final KeyIterator[] keyIterators;

    SharedAlphaData() {
        this.dataWrappers = new DataWrapper[KeyMode.values().length];
        this.keyIterators = new KeyIterator[KeyMode.values().length];
        for (KeyMode mode : KeyMode.values()) {
            int idx = mode.ordinal();
            this.dataWrappers[idx] = new DataWrapper();
            this.keyIterators[idx] = new KeyIterator(mode);
        }
    }

    @Override
    public void insert(FieldToValueHandle key, FactHandleVersioned fact) {
        get(KeyMode.KNOWN_UNKNOWN).add(fact);
    }

    private DataWrapper get(KeyMode mode) {
        return dataWrappers[mode.ordinal()];
    }

    @Override
    public ReIterator<MemoryKey> iterator(KeyMode mode) {
        return this.keyIterators[mode.ordinal()];
    }

    @Override
    public ReIterator<FactHandleVersioned> iterator(KeyMode mode, MemoryKey row) {
        return new CollectionReIterator<>(get(mode));
    }

    @Override
    public void clear() {
        for (DataWrapper wrapper : this.dataWrappers) {
            wrapper.clear();
        }
    }

    @Override
    public void commitChanges() {
        DataWrapper delta = get(KeyMode.KNOWN_UNKNOWN);
        get(KeyMode.MAIN).addAll(delta);
        delta.clear();
    }

    private static class DataWrapper extends LinkedList<FactHandleVersioned> {

        private static final long serialVersionUID = -2837913574833795126L;
    }

    private static class KeyIterator implements ReIterator<MemoryKey> {
        private static final ValueHandle[] EMPTY = new ValueHandle[0];
        private final ValueRowImpl row;
        private boolean hasNext = true;

        KeyIterator(KeyMode mode) {
            this.row = new ValueRowImpl(EMPTY, 0);
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

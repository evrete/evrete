package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.collections.MappedReIterator;
import org.evrete.util.KeysStoreStub;

import java.util.EnumMap;

public class BetaEntryNode extends RuntimeFactTypeKeyed implements BetaMemoryNode<EntryNodeDescriptor> {
    private final EntryNodeDescriptor descriptor;
    private final RuntimeFactType[][] grouping;
    private final EnumMap<KeyMode, KeysStore> stores = new EnumMap<>(KeyMode.class);

    BetaEntryNode(EntryNodeDescriptor node, RuntimeFactTypeKeyed factType) {
        super(factType);
        this.descriptor = node;
        for (KeyMode mode : KeyMode.values()) {
            KeysStore store = new KeysStoreDelegate(getKeyIterators().keyIterator(mode));
            stores.put(mode, store);
        }
        this.grouping = new RuntimeFactType[1][1];
        this.grouping[0][0] = this;
    }

    @Override
    public KeysStore getStore(KeyMode mode) {
        return stores.get(mode);
    }

    @Override
    public RuntimeFactType[][] getGrouping() {
        return grouping;
    }

    @Override
    public EntryNodeDescriptor getDescriptor() {
        return descriptor;
    }


    @Override
    public void clear() {
    }

    @Override
    public void mergeDelta() {
    }

    static class KeysStoreDelegate extends KeysStoreStub {
        private final ReIterator<Entry> entryReIterator;

        KeysStoreDelegate(ReIterator<ValueRow> storage) {
            final DummyEntry entry = new DummyEntry();

            this.entryReIterator = new MappedReIterator<>(storage, valueRows -> {
                entry.arr[0] = valueRows;
                return entry;
            });
        }

        @Override
        @ThreadUnsafe
        public ReIterator<Entry> entries() {
            return entryReIterator;
        }
    }

    private static class DummyEntry implements KeysStore.Entry {
        final ValueRow[] arr = new ValueRow[1];

        @Override
        public ValueRow[] key() {
            return arr;
        }

        @Override
        public KeysStore getNext() {
            throw new UnsupportedOperationException();
        }
    }
}

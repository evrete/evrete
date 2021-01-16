package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.collections.MappedReIterator;
import org.evrete.util.KeysStoreStub;

public class BetaEntryNode extends RuntimeFactTypeKeyed implements BetaMemoryNode<EntryNodeDescriptor> {
    private final KeysStore mainStore;
    private final KeysStore deltaStore;
    private final EntryNodeDescriptor descriptor;
    private final RuntimeFactType[][] grouping;

    BetaEntryNode(EntryNodeDescriptor node, RuntimeFactTypeKeyed factType) {
        super(factType);
        this.descriptor = node;
        this.deltaStore = new KeysStoreDelegate(getKeyIterators().keyIterator(KeyMode.NEW_KEYS_NEW_FACTS));
        this.mainStore = new KeysStoreDelegate(getKeyIterators().keyIterator(KeyMode.KNOWN_KEYS_KNOWN_FACTS));
        this.grouping = new RuntimeFactType[1][1];
        this.grouping[0][0] = this;
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
    public KeysStore getDeltaStore() {
        return deltaStore;
    }

    @Override
    public KeysStore getMainStore() {
        return mainStore;
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

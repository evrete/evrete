package org.evrete.runtime.memory;

import org.evrete.api.*;
import org.evrete.collections.MappedReIterator;
import org.evrete.runtime.EntryNodeDescriptor;
import org.evrete.runtime.RuntimeFactType;
import org.evrete.runtime.RuntimeFactTypeKeyed;

public class BetaEntryNode extends RuntimeFactTypeKeyed implements BetaMemoryNode<EntryNodeDescriptor> {
    private final KeysStore mainStore;
    private final KeysStore deltaStore;
    private final EntryNodeDescriptor descriptor;
    private final RuntimeFactType[][] grouping;

    BetaEntryNode(EntryNodeDescriptor node, RuntimeFactTypeKeyed factType) {
        super(factType);
        this.descriptor = node;
        this.deltaStore = new KeysStoreDelegate(getKeyStorage().deltaNewKeys());
        this.mainStore = new KeysStoreDelegate(getKeyStorage().main());
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
    public void mergeDelta() {
    }

    static class KeysStoreDelegate extends KeysStoreStub {
        private final KeyIterable storage;
        private final ReIterator<Entry> entryReIterator;

        KeysStoreDelegate(KeyIterable storage) {
            this.storage = storage;
            final DummyEntry entry = new DummyEntry();

            this.entryReIterator = new MappedReIterator<>(storage.keyIterator(), valueRows -> {
                entry.arr = valueRows;
                return entry;
            });
        }

        @Override
        public long keyCount() {
            return storage.keyCount();
        }

        @Override
        @ThreadUnsafe
        public ReIterator<Entry> entries() {
            return entryReIterator;
        }
    }

    private static class DummyEntry implements KeysStore.Entry {
        ValueRow[] arr;

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

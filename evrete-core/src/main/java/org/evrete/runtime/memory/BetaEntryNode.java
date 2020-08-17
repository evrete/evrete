package org.evrete.runtime.memory;

import org.evrete.api.KeysStore;
import org.evrete.api.ReIterator;
import org.evrete.api.ThreadUnsafe;
import org.evrete.api.ValueRow;
import org.evrete.api.spi.SharedBetaFactStorage;
import org.evrete.collections.MappedReIterator;
import org.evrete.runtime.RuntimeFactType;
import org.evrete.runtime.RuntimeFactTypeKeyed;
import org.evrete.runtime.structure.EntryNodeDescriptor;

public class BetaEntryNode extends RuntimeFactTypeKeyed implements BetaMemoryNode<EntryNodeDescriptor> {
    private final KeysStore mainStore;
    private final KeysStore deltaStore;
    private final EntryNodeDescriptor descriptor;
    private final RuntimeFactType[][] grouping;

    BetaEntryNode(EntryNodeDescriptor node, RuntimeFactTypeKeyed factType) {
        super(factType);
        this.descriptor = node;
        this.deltaStore = new KeysStoreDelegate(getKeyStorage().delta());
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
        private final SharedBetaFactStorage.Scope storage;
        private final ReIterator<Entry> entryReIterator;

        KeysStoreDelegate(SharedBetaFactStorage.Scope storage) {
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

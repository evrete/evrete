package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.collections.MappedReIterator;
import org.evrete.util.KeysStoreStub;

import java.util.EnumMap;

public class BetaEntryNode implements BetaMemoryNode<EntryNodeDescriptor> {
    private final EntryNodeDescriptor descriptor;
    private final EnumMap<KeyMode, KeysStore> stores = new EnumMap<>(KeyMode.class);

    BetaEntryNode(AbstractKnowledgeSession<?> runtime, EntryNodeDescriptor node) {
        this.descriptor = node;
        for (KeyMode mode : KeyMode.values()) {
            ReIterator<MemoryKey> it = runtime.getMemory().getBetaFactStorage(node.getFactType()).keys(mode);
            KeysStore store = new KeysStoreDelegate(mode, it);
            stores.put(mode, store);
        }
    }

    @Override
    public KeysStore getStore(KeyMode mode) {
        return stores.get(mode);
    }

    @Override
    public EntryNodeDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public void clear() {
    }

    @Override
    public void commitDelta() {
    }

    @Override
    public String toString() {
        return descriptor.getFactType().toString();
    }

    static class KeysStoreDelegate extends KeysStoreStub {
        private final ReIterator<Entry> entryReIterator;


        KeysStoreDelegate(KeyMode keyMode, ReIterator<MemoryKey> storage) {
            final DummyEntry entry = new DummyEntry();
            this.entryReIterator = new MappedReIterator<>(storage, row -> {
                row.setMetaValue(keyMode.ordinal());
                entry.arr[0] = row;
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
        final MemoryKey[] arr = new MemoryKey[1];

        @Override
        public MemoryKey[] key() {
            return arr;
        }

        @Override
        public KeysStore getNext() {
            throw new UnsupportedOperationException();
        }
    }
}

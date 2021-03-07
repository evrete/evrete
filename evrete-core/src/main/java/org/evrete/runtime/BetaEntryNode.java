package org.evrete.runtime;

import org.evrete.api.KeyMode;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;
import org.evrete.collections.MappedReIterator;

import java.util.EnumMap;

public class BetaEntryNode implements BetaMemoryNode {
    private final EntryNodeDescriptor descriptor;
    private final EnumMap<KeyMode, ReIterator<MemoryKey[]>> stores = new EnumMap<>(KeyMode.class);

    BetaEntryNode(AbstractKnowledgeSession<?> runtime, EntryNodeDescriptor node) {
        this.descriptor = node;
        for (KeyMode mode : KeyMode.values()) {
            ReIterator<MemoryKey> it = runtime.getMemory().getBetaFactStorage(node.getFactType()).keys(mode);
            final MemoryKey[] dummyArray = new MemoryKey[1];
            stores.put(mode, new MappedReIterator<>(it, key -> {
                key.setMetaValue(mode.ordinal());
                dummyArray[0] = key;
                return dummyArray;
            }));
        }
    }

    @Override
    public ReIterator<MemoryKey[]> iterator(KeyMode mode) {
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

}

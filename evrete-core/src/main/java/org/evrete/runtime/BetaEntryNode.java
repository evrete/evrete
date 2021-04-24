package org.evrete.runtime;

import org.evrete.api.KeyMode;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;

import java.util.EnumMap;

public class BetaEntryNode implements BetaMemoryNode {
    private final EntryNodeDescriptor descriptor;
    private final EnumMap<KeyMode, ReIterator<MemoryKey>> stores = new EnumMap<>(KeyMode.class);

    BetaEntryNode(AbstractRuleSession<?> runtime, EntryNodeDescriptor node) {
        this.descriptor = node;
        for (KeyMode mode : KeyMode.values()) {
            ReIterator<MemoryKey> it = runtime.getMemory().getBetaFactStorage(node.getFactType()).keys(mode);

            ReIterator<MemoryKey> mapped = new ReIterator<MemoryKey>() {
                @Override
                public long reset() {
                    return it.reset();
                }

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public MemoryKey next() {
                    MemoryKey key = it.next();
                    key.setMetaValue(mode.ordinal());
                    return key;
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };

            stores.put(mode, mapped);
        }
    }

    @Override
    public ReIterator<MemoryKey> iterator(KeyMode mode) {
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

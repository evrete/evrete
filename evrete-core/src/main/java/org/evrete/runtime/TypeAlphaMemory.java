package org.evrete.runtime;

import org.evrete.api.spi.GroupingReteMemory;
import org.evrete.util.GroupingReteMemoryWrapper;

public class TypeAlphaMemory extends GroupingReteMemoryWrapper<DefaultFactHandle> {
    private final AlphaAddress alphaAddress;

    TypeAlphaMemory(GroupingReteMemory<DefaultFactHandle> delegate, AlphaAddress alphaAddress) {
        super(delegate);
        this.alphaAddress = alphaAddress;
    }

    public AlphaAddress getAlphaAddress() {
        return alphaAddress;
    }
}

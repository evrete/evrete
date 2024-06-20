package org.evrete.runtime;

import org.evrete.api.spi.DeltaGroupedFactStorage;
import org.evrete.util.DeltaGroupedFactStorageWrapper;

public class TypeAlphaMemory extends DeltaGroupedFactStorageWrapper<DefaultFactHandle> {
    private final AlphaAddress alphaAddress;

    TypeAlphaMemory(DeltaGroupedFactStorage<DefaultFactHandle> delegate, AlphaAddress alphaAddress) {
        super(delegate);
        this.alphaAddress = alphaAddress;
    }

    public AlphaAddress getAlphaAddress() {
        return alphaAddress;
    }
}

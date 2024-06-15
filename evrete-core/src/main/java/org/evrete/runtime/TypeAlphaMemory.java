package org.evrete.runtime;

import org.evrete.api.spi.DeltaGroupedFactStorage;
import org.evrete.util.DeltaGroupedFactStorageWrapper;

public class TypeAlphaMemory extends DeltaGroupedFactStorageWrapper<FactFieldValues, DefaultFactHandle> {
    private final AlphaAddress alphaAddress;

    TypeAlphaMemory(DeltaGroupedFactStorage<FactFieldValues, DefaultFactHandle> delegate, AlphaAddress alphaAddress) {
        super(delegate);
        this.alphaAddress = alphaAddress;
    }

    public AlphaAddress getAlphaAddress() {
        return alphaAddress;
    }
}

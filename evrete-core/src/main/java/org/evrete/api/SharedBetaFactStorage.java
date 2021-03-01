package org.evrete.api;

import org.evrete.api.spi.InnerFactMemory;

public interface SharedBetaFactStorage extends InnerFactMemory {
    ReIterator<MemoryKey> iterator(KeyMode keyMode);

    ReIterator<FactHandleVersioned> iterator(KeyMode mode, MemoryKey row);

    void insert(FieldToValueHandle key, FactHandleVersioned value);
}

package org.evrete.api;

public interface SharedBetaFactStorage extends InnerFactMemory {
    ReIterator<MemoryKey> keys(KeyMode keyMode);

    ReIterator<FactHandleVersioned> values(KeyMode mode, MemoryKey key);

    void insert(FieldToValueHandle key, FactHandleVersioned value);
}

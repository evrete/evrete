package org.evrete.runtime;

import org.evrete.api.*;

class RuntimeFactType extends FactType {
    private final TypeMemory typeMemory;
    private final KeyedFactStorage keyedFactStorage;

    RuntimeFactType(FactType type, SessionMemory memory) {
        super(type);
        this.typeMemory = memory.get(type.type());
        this.keyedFactStorage = memory.getBetaFactStorage(type.getMemoryAddress());
    }

    FactRecord get(FactHandle handle) {
        return typeMemory.getStoredRecord(handle);
    }

    ReIterator<FactHandleVersioned> factIterator(KeyMode mode, MemoryKey key) {
        return keyedFactStorage.values(mode, key);
    }
}

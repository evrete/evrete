package org.evrete.runtime;

import org.evrete.api.*;

class RuntimeFactType extends FactType {
    private final AbstractRuleSession<?> runtime;
    private final KeyedFactStorage keyedFactStorage;

    RuntimeFactType(FactType type, SessionMemory memory) {
        super(type);
        this.runtime = memory.getRuntime();
        this.keyedFactStorage = memory.getBetaFactStorage(type.getMemoryAddress());
    }

    FactRecord get(FactHandle handle) {
        return runtime.getFactRecord(handle);
    }

    ReIterator<FactHandleVersioned> factIterator(KeyMode mode, MemoryKey key) {
        return keyedFactStorage.values(mode, key);
    }
}

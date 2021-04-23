package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.KeyMode;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;

interface RhsFactGroup {
    ReIterator<MemoryKey> keyIterator(boolean delta);

    default ReIterator<FactHandleVersioned> factIterator(RuntimeFactType type, KeyMode mode, MemoryKey key) {
        return type.factIterator(mode, key);
    }


    RuntimeFactType[] types();
}

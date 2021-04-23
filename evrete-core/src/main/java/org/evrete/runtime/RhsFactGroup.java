package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;

interface RhsFactGroup {
    ReIterator<MemoryKey> keyIterator(boolean delta);

    ReIterator<FactHandleVersioned> factIterator(FactType type, MemoryKey row);

    RuntimeFactType[] types();
}

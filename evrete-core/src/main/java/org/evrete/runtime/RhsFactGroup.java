package org.evrete.runtime;

import org.evrete.api.KeyMode;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;

interface RhsFactGroup {

    ReIterator<MemoryKey> keyIterator(KeyMode mode);

    RuntimeFactType[] types();

    Mask<MemoryAddress> getMemoryMask();
}

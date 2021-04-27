package org.evrete.runtime;

import org.evrete.api.KeyMode;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;
import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.Mask;

interface RhsFactGroup {

    ReIterator<MemoryKey> keyIterator(KeyMode mode);

    RuntimeFactType[] types();

    Mask<MemoryAddress> getMemoryMask();
}

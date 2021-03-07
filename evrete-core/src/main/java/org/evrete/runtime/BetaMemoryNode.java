package org.evrete.runtime;

import org.evrete.api.KeyMode;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;

interface BetaMemoryNode {

    ReIterator<MemoryKey[]> iterator(KeyMode mode);

    void commitDelta();

    void clear();

    NodeDescriptor getDescriptor();

}

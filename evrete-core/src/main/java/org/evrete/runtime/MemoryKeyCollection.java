package org.evrete.runtime;

import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterable;

public interface MemoryKeyCollection extends ReIterable<MemoryKey> {

    void clear();

    void add(MemoryKey key);
}

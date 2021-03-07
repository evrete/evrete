package org.evrete.runtime;

import org.evrete.api.IntToMemoryKey;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;

public interface ZStoreI {
    ReIterator<MemoryKey[]> entries();

    void clear();

    void append(ZStoreI other);

    void save(IntToMemoryKey key);
}

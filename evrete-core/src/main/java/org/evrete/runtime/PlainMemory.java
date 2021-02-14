package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.ReIterator;
import org.evrete.api.spi.InnerFactMemory;

public interface PlainMemory extends InnerFactMemory {
    ReIterator<FactHandleVersioned> mainIterator();

    ReIterator<FactHandleVersioned> deltaIterator();

    boolean hasChanges();

}

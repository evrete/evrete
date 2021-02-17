package org.evrete.api.spi;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.FieldToValueHandle;

public interface InnerFactMemory {

    void insert(FactHandleVersioned value, FieldToValueHandle key);

    void commitChanges();
}

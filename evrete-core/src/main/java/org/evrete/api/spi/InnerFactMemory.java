package org.evrete.api.spi;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.FieldToValue;

public interface InnerFactMemory {

    void insert(FactHandleVersioned value, FieldToValue key);

    void commitChanges();
}

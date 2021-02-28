package org.evrete.api;

import org.evrete.api.spi.InnerFactMemory;

//TODO !!!! javadoc, cleanup
public interface SharedPlainFactStorage extends InnerFactMemory {

    int size();

    ReIterator<FactHandleVersioned> iterator();

    void insert(FieldToValueHandle key, FactHandleVersioned fact);

    void insert(SharedPlainFactStorage other);
}

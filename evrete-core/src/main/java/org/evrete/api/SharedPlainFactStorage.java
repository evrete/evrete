package org.evrete.api;

import org.evrete.api.spi.InnerFactMemory;

//TODO !!!! javadoc, cleanup
public interface SharedPlainFactStorage extends InnerFactMemory, ReIterable<FactHandleVersioned> {

    //void insert(FactHandleVersioned fact);

    void clear();

    int size();

    void insert(SharedPlainFactStorage other);
}

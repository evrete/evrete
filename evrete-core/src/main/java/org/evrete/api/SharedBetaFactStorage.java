package org.evrete.api;

import org.evrete.api.spi.InnerFactMemory;

public interface SharedBetaFactStorage extends InnerFactMemory {
    ReIterator<ValueRow> iterator(KeyMode keyMode);

    ReIterator<FactHandleVersioned> iterator(KeyMode mode, ValueRow row);
    //void delete(RuntimeFact fact);

    //void insert(FactHandle fact, FieldToValue values);

}

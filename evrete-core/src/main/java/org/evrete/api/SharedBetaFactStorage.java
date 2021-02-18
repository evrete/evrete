package org.evrete.api;

import org.evrete.api.spi.InnerFactMemory;

public interface SharedBetaFactStorage extends InnerFactMemory, KeyReIterables<ValueRow> {

    //void delete(RuntimeFact fact);

    //void insert(FactHandle fact, FieldToValue values);

}

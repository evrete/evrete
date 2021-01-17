package org.evrete.api;

public interface SharedBetaFactStorage extends Memory, KeyReIterables<ValueRow> {

    void delete(RuntimeFact fact);

    void insert(RuntimeFact fact);

    void clear();
}

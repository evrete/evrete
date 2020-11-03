package org.evrete.api;

import java.util.Collection;
import java.util.function.Predicate;

public interface SharedBetaFactStorage extends Memory, KeyReIterables<ValueRow> {

    void ensureDeltaCapacity(int insertCount);

    //boolean delete(RuntimeFact fact);

    void insert(Collection<? extends RuntimeFact> collection, Predicate<RuntimeFact> predicate);

    void delete(RuntimeFact fact);

    void insert(RuntimeFact fact);

    void delete(Collection<? extends RuntimeFact> collection, Predicate<RuntimeFact> predicate);

    void clearDeletedKeys();

    boolean hasDeletedKeys();

    boolean isKeyDeleted(ValueRow row);

    void clear();
}

package org.evrete.api;

import java.util.Collection;
import java.util.function.Predicate;

public interface SharedBetaFactStorage extends Memory, KeyReIterables<ValueRow> {

    void ensureDeltaCapacity(int insertCount);

    @Deprecated
    void insert(Collection<? extends RuntimeFact> collection, Predicate<RuntimeFact> predicate);

    void delete(RuntimeFact fact);

    void insert(RuntimeFact fact);

    @Deprecated
    void delete(Collection<? extends RuntimeFact> collection, Predicate<RuntimeFact> predicate);

    @Deprecated
    void clearDeletedKeys();

    @Deprecated
    boolean hasDeletedKeys();

    @Deprecated
    boolean isKeyDeleted(ValueRow row);

    void clear();
}

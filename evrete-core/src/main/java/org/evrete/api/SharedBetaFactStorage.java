package org.evrete.api;

import java.util.Collection;
import java.util.function.Predicate;

public interface SharedBetaFactStorage extends Memory, KeyReIterables<ValueRow> {

    void ensureDeltaCapacity(int insertCount);

    @Deprecated
    default void insert(Collection<? extends RuntimeFact> collection, Predicate<RuntimeFact> predicate) {
        throw new UnsupportedOperationException();
    }

    void delete(RuntimeFact fact);

    void insert(RuntimeFact fact);

    @Deprecated
    default void delete(Collection<? extends RuntimeFact> collection, Predicate<RuntimeFact> predicate) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    default void clearDeletedKeys() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    default boolean hasDeletedKeys() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    default boolean isKeyDeleted(ValueRow row) {
        throw new UnsupportedOperationException();
    }

    void clear();
}

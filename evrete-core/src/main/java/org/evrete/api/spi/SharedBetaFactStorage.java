package org.evrete.api.spi;

import org.evrete.api.*;

import java.util.Collection;
import java.util.function.Predicate;

public interface SharedBetaFactStorage extends Memory, KeyReIterables<ValueRow> {

    void ensureDeltaCapacity(int insertCount);

    boolean delete(RuntimeFact fact);

    void insert(Collection<? extends RuntimeFact> collection, Predicate<RuntimeFact> predicate);

    /**
     * <p>
     * This method reads fact's key (values of specific fields) and saves
     * the object under this key directly in the main repository.
     * </p>
     *
     * @param fact fact to insert
     */
    void insertDirect(RuntimeFact fact);

    void clearDeletedKeys();

    boolean hasDeletedKeys();

    boolean isKeyDeleted(ValueRow row);

    void clear();
}

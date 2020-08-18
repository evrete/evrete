package org.evrete.api.spi;

import org.evrete.api.KeyIterable;
import org.evrete.api.RuntimeFact;
import org.evrete.api.ValueRow;

public interface SharedBetaFactStorage {

    void ensureDeltaCapacity(int insertCount);

    boolean delete(RuntimeFact fact);

    /**
     * <p>
     * The method first reads the fact's key (values of specific fields) and
     * checks if any of main and delta repositories contain such a key. If
     * they don't, a new key will be created in the delta repository, the fact
     * will be stored there under this key, and the method will return true.
     * If the key is already known, the fact will be appended to the corresponding
     * repository, and the method will return false.
     * </p>
     *
     * @param fact fact to process
     * @return true if the fact's field values (the storage key) are unknown to either
     * delta and main scopes
     */
    boolean insert(RuntimeFact fact);

    /**
     * <p>
     * This method reads fact's key (values of specific fields) and saves
     * the object under this key directly in the main repository.
     * </p>
     *
     * @param fact fact to insert
     */
    void insertDirect(RuntimeFact fact);

    void mergeDelta();

    void clearDeletedKeys();

    boolean hasDeletedKeys();

    boolean isKeyDeleted(ValueRow row);

    Scope delta();

    Scope main();

    void clear();

    interface Scope extends KeyIterable {

        long keyCount();
    }
}

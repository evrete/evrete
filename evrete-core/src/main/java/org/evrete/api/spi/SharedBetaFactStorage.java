package org.evrete.api.spi;

import org.evrete.api.KeyIterable;
import org.evrete.api.RuntimeFact;
import org.evrete.api.ValueRow;

public interface SharedBetaFactStorage {

    void ensureExtraCapacity(int insertCount);

    ValueRow delete(RuntimeFact fact);

    boolean insert(RuntimeFact fact);

    void mergeDelta();

    Scope delta();

    Scope main();

    void clear();

    interface Scope extends KeyIterable {
        long keyCount();
    }
}

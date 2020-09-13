package org.evrete.spi.minimal;

import org.evrete.api.RuntimeFact;
import org.evrete.api.SharedPlainFactStorage;
import org.evrete.collections.FastIdentityHashSet;

public class SharedAlphaData extends FastIdentityHashSet<RuntimeFact> implements SharedPlainFactStorage {

    @Override
    public void delete(RuntimeFact fact) {
        super.remove(fact);
    }

    @Override
    public void insert(RuntimeFact fact) {
        super.addNoResize(fact);
    }


}

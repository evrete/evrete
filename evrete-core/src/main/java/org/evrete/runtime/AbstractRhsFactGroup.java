package org.evrete.runtime;

import org.evrete.api.FactHandle;
import org.evrete.api.FactHandleVersioned;
import org.evrete.api.ReIterator;

abstract class AbstractRhsFactGroup {
    final int lastIndex;
    final FactIterationState[] state;
    private final SessionMemory runtime;

    AbstractRhsFactGroup(SessionMemory runtime, FactIterationState[] state) {
        this.state = state;
        this.runtime = runtime;
        this.lastIndex = state.length - 1;
    }

    final boolean next(FactIterationState state, ReIterator<FactHandleVersioned> it) {
        FactHandleVersioned v = it.next();
        FactHandle handle = v.getHandle();
        FactRecord fact = state.typeMemory.getFact(handle);
        if (fact == null || fact.getVersion() != v.getVersion()) {
            it.remove();
            return false;
        } else {
            state.handle = handle;
            state.value = fact.instance;
            return true;
        }
    }

}

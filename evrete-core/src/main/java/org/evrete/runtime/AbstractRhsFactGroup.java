package org.evrete.runtime;

abstract class AbstractRhsFactGroup {
/*
    final int lastIndex;
    final FactIterationState[] state;

    AbstractRhsFactGroup(FactIterationState[] state) {
        this.state = state;
        this.lastIndex = state.length - 1;
    }

    AbstractRhsFactGroup() {
        this.state = null;
        this.lastIndex = - 1;
    }

    static boolean next(FactIterationState state, ReIterator<FactHandleVersioned> it) {
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
*/

}

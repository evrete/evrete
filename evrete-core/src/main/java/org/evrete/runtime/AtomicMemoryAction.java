package org.evrete.runtime;

import org.evrete.api.Action;
import org.evrete.api.FactHandle;

class AtomicMemoryAction {
    final FactHandle handle;
    Action action;
    LazyInsertState factRecord;

    AtomicMemoryAction(Action action, FactHandle handle, LazyInsertState factRecord) {
        this.action = action;
        this.handle = handle;
        this.factRecord = factRecord;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtomicMemoryAction that = (AtomicMemoryAction) o;
        return handle.equals(that.handle);
    }

    @Override
    public int hashCode() {
        return handle.hashCode();
    }

    @Override
    public String toString() {
        return "{action=" + action +
                ", handle=" + handle +
                ", rec=" + factRecord +
                '}';
    }
}

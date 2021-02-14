package org.evrete.helper;

import org.evrete.api.FactHandle;

public class FactEntry {
    private final FactHandle handle;
    private final Object fact;

    FactEntry(FactHandle handle, Object fact) {
        this.handle = handle;
        this.fact = fact;
    }

    public FactHandle getHandle() {
        return handle;
    }

    public Object getFact() {
        return fact;
    }

    @Override
    public String toString() {
        return "{" +
                "handle=" + handle +
                ", fact=" + fact +
                '}';
    }
}

package org.evrete.api;

import java.io.Serializable;

public final class FactHandleVersioned implements Serializable {
    private static final long serialVersionUID = 7190204658108518551L;
    private final int version;
    private final FactHandle handle;

    public FactHandleVersioned(FactHandle handle, int version) {
        this.handle = handle;
        this.version = version;
    }

    public FactHandleVersioned(FactHandle handle) {
        this(handle, 0);
    }

    public int getVersion() {
        return version;
    }

    public FactHandle getHandle() {
        return handle;
    }

    @Override
    public String toString() {
        return "{v=" + version +
                ", h=" + handle +
                '}';
    }
}

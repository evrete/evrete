package org.evrete.util;

import org.evrete.runtime.PreHashed;

public abstract class AbstractIndex extends PreHashed implements Indexed {
    private final int index;

    protected AbstractIndex(int index, int hashCode) {
        super(hashCode);
        this.index = index;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return "{" +
                "idx=" + index +
                '}';
    }
}

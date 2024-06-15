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

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        AbstractIndex that = (AbstractIndex) o;
//        return index == that.index;
//    }

    public static int hash(AbstractIndex index1, AbstractIndex index2) {
        return index1.index * 31 + index2.index;
    }
}

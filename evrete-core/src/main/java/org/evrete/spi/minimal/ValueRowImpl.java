package org.evrete.spi.minimal;

import org.evrete.api.MemoryKey;
import org.evrete.api.ValueHandle;

import java.util.Arrays;

class ValueRowImpl implements MemoryKey {
    private final ValueHandle[] data;
    private final int hash;
    //TODO try excluding the 'volatile' keyword, it looks like it's safe
    private volatile transient int transientValue;

    ValueRowImpl(ValueHandle[] data, int hash) {
        this.data = data;
        this.hash = hash;
    }

    @Override
    public int getMetaValue() {
        return transientValue;
    }

    @Override
    public void setMetaValue(int i) {
        this.transientValue = i;
    }

    @Override
    public String toString() {
        return Arrays.toString(data) + "/" + transientValue;
    }

    @Override
    public ValueHandle get(int i) {
        return data[i];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValueRowImpl other = (ValueRowImpl) o;
        return other.transientValue == this.transientValue && MiscUtils.sameData(other.data, data);
    }

    @Override
    public final int hashCode() {
        return hash;
    }
}

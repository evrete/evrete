package org.evrete.spi.minimal;

import org.evrete.api.ActiveField;
import org.evrete.api.IntToValueHandle;
import org.evrete.api.MemoryKey;
import org.evrete.api.ValueHandle;

import java.util.Arrays;

class MemoryKeyMulti implements MemoryKey {
    private static final ValueHandle[] EMPTY = new ValueHandle[0];
    private final ValueHandle[] data;
    private final int hash;
    private transient int transientValue;

    MemoryKeyMulti() {
        this.data = EMPTY;
        this.hash = 0;
    }

    MemoryKeyMulti(ActiveField[] fields, IntToValueHandle key, int hash) {
        this.data = new ValueHandle[fields.length];
        for (int i = 0; i < fields.length; i++) {
            this.data[i] = key.apply(i);
        }
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
        MemoryKeyMulti other = (MemoryKeyMulti) o;
        return other.transientValue == this.transientValue && Arrays.equals(other.data, data);
    }

    @Override
    public final int hashCode() {
        return hash;
    }
}

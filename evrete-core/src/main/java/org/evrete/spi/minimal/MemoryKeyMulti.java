package org.evrete.spi.minimal;

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

    MemoryKeyMulti(int fieldCount, MemoryKeyHashed key) {
        this.data = new ValueHandle[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            this.data[i] = key.values.apply(i);
        }
        this.hash = key.hash;
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
        return Arrays.equals(other.data, data);
    }

    @Override
    public final int hashCode() {
        return hash;
    }
}

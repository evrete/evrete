package org.evrete.spi.minimal;

import org.evrete.api.ActiveField;
import org.evrete.api.FieldToValueHandle;
import org.evrete.api.MemoryKey;
import org.evrete.api.ValueHandle;

import java.util.Arrays;

class MemoryKeyMulti implements MemoryKey {
    private final ValueHandle[] data;
    private final int hash;
    private transient int transientValue;

    MemoryKeyMulti(ValueHandle[] data, int hash) {
        this.data = data;
        this.hash = hash;
    }

    MemoryKeyMulti(ActiveField[] fields, FieldToValueHandle key, int hash) {
        this.data = new ValueHandle[fields.length];
        for (int i = 0; i < fields.length; i++) {
            this.data[i] = key.apply(fields[i]);
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

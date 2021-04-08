package org.evrete.spi.minimal;

import org.evrete.api.MemoryKey;
import org.evrete.api.ValueHandle;

import java.util.Objects;

class MemoryKeyImplPlain implements MemoryKey {
    final ValueHandle data;
    private final int hash;
    private transient int transientValue;

    MemoryKeyImplPlain(ValueHandle data, int hash) {
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
        return data + "/" + transientValue;
    }

    @Override
    public ValueHandle get(int i) {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemoryKeyImplPlain other = (MemoryKeyImplPlain) o;
        return other.transientValue == this.transientValue && Objects.equals(other.data, data);
    }

    @Override
    public final int hashCode() {
        return hash;
    }
}

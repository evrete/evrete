package org.evrete.spi.minimal;

import org.evrete.api.KeyMode;
import org.evrete.api.MemoryKey;
import org.evrete.api.ValueHandle;

import java.util.Objects;

class MemoryKeySingle implements MemoryKey {
    final ValueHandle data;
    private final int hash;
    private transient int transientValue;

    MemoryKeySingle(ValueHandle data, int hash) {
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
        if (transientValue < 0) {
            return data + "/UNKNOWN";
        } else {
            return data + "/" + KeyMode.values()[transientValue];
        }
    }

    @Override
    public ValueHandle get(int i) {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemoryKeySingle other = (MemoryKeySingle) o;
        return Objects.equals(other.data, data);
    }

    @Override
    public final int hashCode() {
        return hash;
    }
}

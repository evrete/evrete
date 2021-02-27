package org.evrete.spi.minimal;

import org.evrete.api.ValueHandle;

import java.util.Arrays;

class ValueHandleImpl implements ValueHandle {
    final int[] data;
    private final int hash;

    ValueHandleImpl(int[] data) {
        this.data = data;
        this.hash = Arrays.hashCode(data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValueHandleImpl handle = (ValueHandleImpl) o;
        return Arrays.equals(data, handle.data);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return Arrays.toString(data);
    }
}

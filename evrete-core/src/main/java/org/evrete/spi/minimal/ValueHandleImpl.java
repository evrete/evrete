package org.evrete.spi.minimal;

import org.evrete.api.ValueHandle;

import java.util.Objects;

class ValueHandleImpl implements ValueHandle {
    final Object value;

    ValueHandleImpl(Object value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValueHandleImpl handle = (ValueHandleImpl) o;
        return Objects.equals(value, handle.value);
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(value);
    }

    @Override
    public String toString() {
        return value == null ? "null" : value.toString();
    }
}

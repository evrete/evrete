package org.evrete.spi.minimal;

import org.evrete.api.FieldValue;

import java.util.Objects;

class FieldValueImpl implements FieldValue {
    final Object value;

    FieldValueImpl(Object value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldValueImpl that = (FieldValueImpl) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return value == null ? 0 : value.hashCode();
    }

    @Override
    public String toString() {
        return Objects.toString(value);
    }
}

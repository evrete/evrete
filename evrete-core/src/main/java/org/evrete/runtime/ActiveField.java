package org.evrete.runtime;

import org.evrete.api.Type;
import org.evrete.api.TypeField;

import java.util.Objects;

/**
 * A wrapper for TypeField that will actually be in use
 * by the runtime. Unused fields will not get wrapped, thus avoiding unnecessary reads.
 */
public final class ActiveField implements TypeField {
    public static final ActiveField[] ZERO_ARRAY = new ActiveField[0];
    private final TypeField delegate;
    private final int valueIndex;

    public ActiveField(TypeField delegate, int valueIndex) {
        this.delegate = delegate;
        this.valueIndex = valueIndex;
    }

    public int getValueIndex() {
        return valueIndex;
    }

    public TypeField getDelegate() {
        return delegate;
    }

    @Override
    public Class<?> getValueType() {
        return delegate.getValueType();
    }

    @Override
    public Object readValue(Object subject) {
        return delegate.readValue(subject);
    }

    @Override
    public Type<?> getDeclaringType() {
        return delegate.getDeclaringType();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActiveField that = (ActiveField) o;
        return valueIndex == that.valueIndex &&
                delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate, valueIndex);
    }

    @Override
    public String toString() {
        return "ActiveField{" +
                "delegate=" + delegate +
                ", valueIndex=" + valueIndex +
                '}';
    }
}

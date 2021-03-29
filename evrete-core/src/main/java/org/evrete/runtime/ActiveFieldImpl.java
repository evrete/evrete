package org.evrete.runtime;

import org.evrete.api.ActiveField;
import org.evrete.api.Type;
import org.evrete.api.TypeField;

final class ActiveFieldImpl implements ActiveField {
    private final TypeField delegate;
    private final int valueIndex;

    ActiveFieldImpl(TypeField delegate, int valueIndex) {
        this.delegate = delegate;
        this.valueIndex = valueIndex;
    }

    @Override
    public int getId() {
        return delegate.getId();
    }

    @Override
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
    public <T> T readValue(Object subject) {
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
        ActiveFieldImpl that = (ActiveFieldImpl) o;
        return valueIndex == that.valueIndex &&
                delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode() + valueIndex;
    }

    @Override
    public String toString() {
        return "{" +
                "index=" + valueIndex +
                ", delegate='" + delegate.getName() +
                "'}";
    }
}

package org.evrete.runtime;

import org.evrete.api.TypeField;

final class ActiveFieldImpl implements ActiveField {
    private final TypeField delegate;
    private final int valueIndex;

    ActiveFieldImpl(TypeField delegate, int valueIndex) {
        this.delegate = delegate;
        this.valueIndex = valueIndex;
    }

    @Override
    public int fieldId() {
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

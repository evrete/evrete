package org.evrete.runtime;

import org.evrete.api.Type;
import org.evrete.api.TypeField;
import org.evrete.util.AbstractIndex;

/**
 * <p>
 * An indexing wrapper for TypeField that will actually be in use by the runtime.
 * Declared, but unused, fields will not get wrapped, thus avoiding unnecessary value reads.
 * </p>
 */
public final class ActiveField implements TypeField {

    private final ActiveType.Idx type;
    private final int valueIndex;
    private final TypeField delegate;

    public ActiveField(ActiveType.Idx type, TypeField delegate, int valueIndex) {
        this.type = type;
        this.delegate = delegate;
        this.valueIndex = valueIndex;
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
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Type<?> getDeclaringType() {
        return delegate.getDeclaringType();
    }

    //TODO add link to where the method is used
    /**
     * Returns auto-incremented index under which the value of this field is stored in an
     * Object[] array during insert/update operations. The index is unique within the declared type.
     * @return unique index inside the active {@link ActiveType}
     */
    public int valueIndex() {
        return valueIndex;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActiveField that = (ActiveField) o;
        return type == that.type && valueIndex == that.valueIndex;
    }

    @Override
    public int hashCode() {
        return type.getIndex() * 37 + valueIndex;
    }

    @Override
    public String toString() {
        return "{" +
                "'" + getName() + "'/" + getValueType() +
                ", ofType=" + type +
                ", valueIdx=" + valueIndex +
                '}';
    }

    static class Index extends AbstractIndex {
        public Index(int index) {
            super(index, index);
        }
    }
}

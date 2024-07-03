package org.evrete.runtime;

import org.evrete.api.TypeField;
import org.evrete.util.AbstractIndex;

import java.util.Objects;

/**
 * <p>
 * An indexing wrapper for TypeField that will actually be in use by the runtime.
 * Declared, but unused, fields will not get wrapped, thus avoiding unnecessary value reads.
 * </p>
 */
public final class ActiveField  {

    private final ActiveType.Idx type;
    private final int valueIndex;
    private final String name;
    private final Class<?> valueType;

    public ActiveField(ActiveType.Idx type, TypeField delegate, int valueIndex) {
        this.type = type;
        this.name = delegate.getName();
        this.valueIndex = valueIndex;
        this.valueType = delegate.getValueType();
    }

    public Class<?> getValueType() {
        return valueType;
    }

    public String getName() {
        return name;
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
        return type.getIndex() == that.type.getIndex() && Objects.equals(name, that.name);
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

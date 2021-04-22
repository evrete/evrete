package org.evrete.runtime;

import org.evrete.api.TypeField;

import java.io.Serializable;

/**
 * <p>
 * A wrapper for TypeField that will actually be in use by the runtime. Declared, but unused, fields will not get
 * wrapped, thus avoiding unnecessary value reads.
 * </p>
 */
public final class ActiveField implements Serializable {
    public static final ActiveField[] ZERO_ARRAY = new ActiveField[0];

    private static final long serialVersionUID = 1318511720324319967L;
    private final int valueIndex;
    private final int fieldId;
    private final int type;

    ActiveField(TypeField delegate, int valueIndex) {
        this.valueIndex = valueIndex;
        this.fieldId = delegate.getId();
        this.type = delegate.getDeclaringType().getId();
    }

    int field() {
        return fieldId;
    }

    int type() {
        return type;
    }

    /**
     * @return index under which the value of this field is stored during insert/update in an Object[] array.
     * @see FieldToValueHandle
     */
    int getValueIndex() {
        return valueIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActiveField that = (ActiveField) o;
        return valueIndex == that.valueIndex && fieldId == that.fieldId;
    }

    @Override
    public int hashCode() {
        return fieldId * 37 + valueIndex;
    }

    @Override
    public String toString() {
        return "{" +
                "index=" + valueIndex +
                ", delegate='" + fieldId +
                "'}";
    }
}

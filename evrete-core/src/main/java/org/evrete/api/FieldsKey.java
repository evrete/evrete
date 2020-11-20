package org.evrete.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

public final class FieldsKey {
    private static final Comparator<ActiveField> DEFAULT_COMPARATOR = Comparator.comparing(ActiveField::getValueIndex);
    private final ActiveField[] fields;
    private final Type<?> type;

    public FieldsKey(Type<?> type, Collection<ActiveField> collection) {
        this(type, collection.toArray(ActiveField.ZERO_ARRAY), DEFAULT_COMPARATOR);
    }

    private FieldsKey(Type<?> type, ActiveField[] arr, Comparator<ActiveField> comparator) {
        this.fields = arr;
        Arrays.sort(this.fields, comparator);
        this.type = type;
    }

    public Type<?> getType() {
        return type;
    }

    public int size() {
        return fields.length;
    }

    public ActiveField[] getFields() {
        return fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldsKey that = (FieldsKey) o;
        return type.equals(that.type) && Arrays.equals(fields, that.fields);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(fields);
    }

    @Override
    public String toString() {
        return type.getJavaType().getSimpleName() + Arrays.toString(fields);
    }
}

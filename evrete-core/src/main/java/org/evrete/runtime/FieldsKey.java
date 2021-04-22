package org.evrete.runtime;

import org.evrete.api.Type;

import java.util.Arrays;

public final class FieldsKey {
    private final ActiveField[] fields;
    private final Type<?> type;
    private final int id;

    FieldsKey(int id, Type<?> type, ActiveField[] arr) {
        this.id = id;
        this.fields = arr;
        this.type = type;
    }

    public int getId() {
        return id;
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
        return Arrays.toString(fields);
    }
}

package org.evrete.runtime;

import java.util.Arrays;

public final class FactFieldValues extends PreHashed{
    private final Object[] values;

    public FactFieldValues(Object[] values) {
        super(Arrays.hashCode(values));
        this.values = values;
    }

    public int size() {
        return values.length;
    }

    public Object valueAt(int index) {
        return values[index];
    }

    private boolean equalsTo(FactFieldValues that) {
        return Arrays.equals(values, that.values);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return equalsTo((FactFieldValues) o);
    }

    @Override
    public String toString() {
        String[] types = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            types[i] = value == null ? null : value.getClass().getName();
        }
        return Arrays.toString(values) + "/" + Arrays.toString(types);
    }

}

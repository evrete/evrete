package org.evrete.runtime;

import org.evrete.api.FieldToValue;

import java.util.Arrays;

class FactRecord implements FieldToValue {
    final Object instance;
    private final Object[] fieldValues;
    private int version = 0;

    FactRecord(Object instance, Object[] fieldValues) {
        this.instance = instance;
        this.fieldValues = fieldValues;
    }

    @Override
    public Object apply(ActiveField activeField) {
        return fieldValues[activeField.getValueIndex()];
    }

    public int getVersion() {
        return version;
    }

    void updateVersion(int newVersion) {
        this.version = newVersion;
    }

    @Override
    public String toString() {
        return "{instance=" + instance +
                ", version=" + version +
                ", values=" + Arrays.toString(fieldValues) +
                '}';
    }
}

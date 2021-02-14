package org.evrete.runtime;

import org.evrete.api.FieldToValue;

import java.util.Arrays;

class FactRecord implements FieldToValue {
    final Object instance;
    private Object[] fieldValues;
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

    final void appendValue(ActiveField field, Object value) {
        assert fieldValues.length == field.getValueIndex();
        this.fieldValues = Arrays.copyOf(this.fieldValues, fieldValues.length + 1);
        this.fieldValues[field.getValueIndex()] = value;
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

package org.evrete.runtime;

import org.evrete.api.ActiveField;
import org.evrete.api.FieldToValueHandle;
import org.evrete.api.ValueHandle;

import java.util.Arrays;

class FactRecord implements FieldToValueHandle {
    final Object instance;
    private ValueHandle[] fieldValues;
    private int version = 0;

    FactRecord(Object instance, ValueHandle[] fieldValues) {
        this.instance = instance;
        this.fieldValues = fieldValues;
    }

    @Override
    public ValueHandle apply(ActiveField activeField) {
        return fieldValues[activeField.getValueIndex()];
    }

    public int getVersion() {
        return version;
    }

    final void appendValue(ActiveField field, ValueHandle value) {
        assert fieldValues.length == field.getValueIndex();
        this.fieldValues = Arrays.copyOf(this.fieldValues, fieldValues.length + 1);
        this.fieldValues[field.getValueIndex()] = value;
    }

    void updateVersion(int newVersion) {
        this.version = newVersion;
    }

    ValueHandle[] getFieldValues() {
        return fieldValues;
    }

    @Override
    public String toString() {
        return "{instance=" + instance +
                ", version=" + version +
                ", values=" + Arrays.toString(fieldValues) +
                '}';
    }
}

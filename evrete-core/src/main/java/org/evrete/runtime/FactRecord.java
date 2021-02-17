package org.evrete.runtime;

import org.evrete.api.FieldToValue;
import org.evrete.api.FieldToValueHandle;
import org.evrete.api.ValueHandle;
import org.evrete.api.ValueResolver;

import java.util.Arrays;

class FactRecord implements FieldToValueHandle {
    final Object instance;
    private Object[] fieldValues1;
    private ValueHandle[] fieldValues;
    private int version = 0;

    FactRecord(Object instance, ValueHandle[] fieldValues) {
        this.instance = instance;
        this.fieldValues = fieldValues;
    }

    FieldToValue values(ValueResolver valueResolver) {
        return new FieldToValue() {
            @Override
            public Object apply(ActiveField activeField) {
                ValueHandle handle = fieldValues[activeField.getValueIndex()];
                return valueResolver.getValue(handle);
            }
        };
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

    @Override
    public String toString() {
        return "{instance=" + instance +
                ", version=" + version +
                ", values=" + Arrays.toString(fieldValues) +
                '}';
    }
}

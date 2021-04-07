package org.evrete.runtime;

import org.evrete.api.ActiveField;
import org.evrete.api.FieldToValue;

class LazyInsertState {
    final FactRecord record;
    private final Object[] transientFieldValues;
    private final FieldToValue values;

    LazyInsertState(FactRecord record, Object[] fieldValues) {
        this.record = record;
        this.transientFieldValues = fieldValues;
        this.values = new FieldToValue() {
            @Override
            public Object apply(ActiveField activeField) {
                return transientFieldValues[activeField.getValueIndex()];
            }
        };
    }


    public FieldToValue getValues() {
        return values;
    }


    @Override
    public String toString() {
        return record.toString();
    }
}

package org.evrete.runtime;

import org.evrete.api.ActiveField;
import org.evrete.api.FieldToValue;
import org.evrete.util.Bits;

class LazyInsertState {
    final FactRecord record;
    private final Object[] transientFieldValues;
    private final FieldToValue values;
    private final Bits alphaTests;

    LazyInsertState(FactRecord record, Bits alphaTests, Object[] fieldValues) {
        this.record = record;
        this.alphaTests = alphaTests;
        this.transientFieldValues = fieldValues;
        this.values = new FieldToValue() {
            @Override
            public Object apply(ActiveField activeField) {
                return transientFieldValues[activeField.getValueIndex()];
            }
        };
    }

    public Bits getAlphaTests() {
        return alphaTests;
    }

    public FieldToValue getValues() {
        return values;
    }

    @Override
    public String toString() {
        return record.toString();
    }
}

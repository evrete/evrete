package org.evrete.runtime;

import org.evrete.api.FieldToValue;
import org.evrete.api.ValueResolver;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.runtime.evaluation.AlphaEvaluator;

public class LazyInsertState {
    final FactRecord record;
    private final Object[] transientFieldValues;
    private final FieldToValue values;

    public LazyInsertState(FactRecord record, Object[] transientFieldValues) {
        this.record = record;
        this.transientFieldValues = transientFieldValues;
        this.values = activeField -> transientFieldValues[activeField.getValueIndex()];
    }

    public LazyInsertState(ValueResolver valueResolver, FactRecord record) {
        this.record = record;
        this.transientFieldValues = new Object[record.getFieldValues().length];
        final boolean[] fieldReadFlags = new boolean[record.getFieldValues().length];
        this.values = new FieldToValue() {
            @Override
            public Object apply(ActiveField activeField) {
                int idx = activeField.getValueIndex();
                if (fieldReadFlags[idx]) {
                    return transientFieldValues[idx];
                } else {
                    Object v = valueResolver.getValue(record.getFieldValues()[idx]);
                    transientFieldValues[idx] = v;
                    fieldReadFlags[idx] = true;
                    return v;
                }
            }
        };
    }

    boolean test(AlphaBucketMeta meta) {
        if (meta.isEmpty()) return true;
        for (int i = 0; i < meta.alphaEvaluators.length; i++) {
            AlphaEvaluator e = meta.alphaEvaluators[i];
            if (e.test(values) != meta.requiredValues[i]) {
                return false;
            }
        }
        return true;
    }
}

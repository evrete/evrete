package org.evrete.runtime.evaluation;

import org.evrete.api.ActiveField;
import org.evrete.api.Evaluator;
import org.evrete.api.LogicallyComparable;

import java.util.function.Predicate;

public class AlphaEvaluator implements LogicallyComparable, Predicate<Object[]> {
    private final Evaluator delegate;
    private final int uniqueId;
    private final int[] valueIndices;

    AlphaEvaluator(int uniqueId, Evaluator e, ActiveField[] fields) {
        this.uniqueId = uniqueId;
        this.delegate = e;

        assert e.descriptor().length == fields.length;

        this.valueIndices = new int[fields.length];
        for (int i = 0; i < fields.length; i++) {
            this.valueIndices[i] = fields[i].getValueIndex();
        }
    }

    public int[] getValueIndices() {
        return valueIndices;
    }

    @Override
    public int compare(LogicallyComparable other) {
        if (other instanceof AlphaEvaluator) {
            AlphaEvaluator ap = (AlphaEvaluator) other;
            if (this.equals(ap)) {
                return RELATION_EQUALS;
            } else {
                return this.delegate.compare(ap.delegate);
            }
        } else {
            return RELATION_NONE;
        }
    }

    @Override
    public boolean test(Object[] values) {
        return delegate.test(value -> values[valueIndices[value]]);
    }

    public int getUniqueId() {
        return uniqueId;
    }

    Evaluator getDelegate() {
        return delegate;
    }

    @Override
    public String toString() {
        return "{id=" + uniqueId +
                ", delegate=" + delegate +
                '}';
    }
}

package org.evrete.runtime.evaluation;

import org.evrete.api.ActiveField;
import org.evrete.api.Evaluator;
import org.evrete.api.LogicallyComparable;
import org.evrete.runtime.builder.FieldReference;

import java.util.function.Predicate;

public class AlphaEvaluator implements LogicallyComparable, Predicate<Object> {
    private final Evaluator delegate;
    private final int uniqueId;
    private final int valueIndex;

    AlphaEvaluator(int uniqueId, Evaluator e, ActiveField field) {
        this.uniqueId = uniqueId;
        this.delegate = e;
        this.valueIndex = field.getValueIndex();
        FieldReference[] descriptor = e.descriptor();
        if (descriptor.length != 1) throw new IllegalStateException();
    }

    public int getValueIndex() {
        return valueIndex;
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
    public boolean test(Object o) {
        return delegate.test(value -> o);
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

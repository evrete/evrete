package org.evrete.runtime.evaluation;

import org.evrete.api.Copyable;
import org.evrete.api.EvaluationListeners;
import org.evrete.api.LogicallyComparable;
import org.evrete.api.ValuesPredicate;

public class AlphaEvaluator extends EvaluatorWrapper implements LogicallyComparable, ValuesPredicate, EvaluationListeners, Copyable<AlphaEvaluator> {
    private final int uniqueId;

    AlphaEvaluator(int uniqueId, EvaluatorWrapper e) {
        super(e);
        this.uniqueId = uniqueId;
    }

    private AlphaEvaluator(AlphaEvaluator other) {
        super(other);
        this.uniqueId = other.uniqueId;
    }

    @Override
    public AlphaEvaluator copyOf() {
        return new AlphaEvaluator(this);
    }

    public int getUniqueId() {
        return uniqueId;
    }
}

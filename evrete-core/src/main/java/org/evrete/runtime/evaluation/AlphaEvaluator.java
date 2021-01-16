package org.evrete.runtime.evaluation;

import org.evrete.api.Copyable;

public class AlphaEvaluator extends EvaluatorWrapper implements Copyable<AlphaEvaluator> {
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

    @Override
    public String toString() {
        return "AlphaEvaluator{" +
                "id=" + uniqueId +
                ", delegate=" + getDelegate() +
                '}';
    }
}

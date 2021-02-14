package org.evrete.runtime.evaluation;

import org.evrete.api.*;
import org.evrete.runtime.ActiveField;

public class AlphaEvaluator implements Copyable<AlphaEvaluator>, EvaluationListeners, LogicallyComparable {
    private final int uniqueId;
    private final ActiveField[] activeDescriptor;
    private final EvaluatorWrapper delegate;

    AlphaEvaluator(int uniqueId, EvaluatorWrapper e, ActiveField[] activeFields) {
        //super(e);
        this.uniqueId = uniqueId;
        this.activeDescriptor = activeFields;
        this.delegate = e;
    }

    private AlphaEvaluator(AlphaEvaluator other) {
        this.uniqueId = other.uniqueId;
        this.activeDescriptor = other.activeDescriptor;
        this.delegate = other.delegate;
    }

    public boolean test(FieldToValue values) {
        return delegate.test((IntToValue) i -> {
            ActiveField f = activeDescriptor[i];
            return values.apply(f);
        });
    }

    @Override
    public AlphaEvaluator copyOf() {
        return new AlphaEvaluator(this);
    }

    int getUniqueId() {
        return uniqueId;
    }

    @Override
    public int compare(LogicallyComparable other) {
        if (other instanceof AlphaEvaluator) {
            return delegate.compare(((AlphaEvaluator) other).delegate);
        } else {
            return delegate.compare(other);
        }
    }

    @Override
    public void addListener(EvaluationListener listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeListener(EvaluationListener listener) {
        delegate.removeListener(listener);
    }

    @Override
    public String toString() {
        return "AlphaEvaluator{" +
                "id=" + uniqueId +
                ", delegate=" + delegate +
                '}';
    }
}

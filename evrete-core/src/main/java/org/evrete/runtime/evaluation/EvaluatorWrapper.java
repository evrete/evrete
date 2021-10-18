package org.evrete.runtime.evaluation;

import org.evrete.api.*;

import java.util.Collection;
import java.util.HashSet;

public class EvaluatorWrapper implements Evaluator, Copyable<EvaluatorWrapper> {
    private Evaluator delegate;
    private Collection<EvaluationListener> listeners = new HashSet<>();
    private final ValuesPredicate verbose = new ValuesPredicate() {
        @Override
        public boolean test(IntToValue intToValue) {
            boolean b = delegate.test(intToValue);
            for (EvaluationListener listener : listeners) {
                listener.fire(delegate, intToValue, b);
            }
            return b;
        }
    };

    private ValuesPredicate active;
    private IntToValue stateValues;

    public EvaluatorWrapper(Evaluator delegate) {
        this.delegate = unwrap(delegate);
        this.stateValues = null;
        updateActiveEvaluator();
    }

    private EvaluatorWrapper(EvaluatorWrapper other) {
        this.delegate = unwrap(other.delegate);
        this.listeners.addAll(other.listeners);
        this.stateValues = null;
        updateActiveEvaluator();
    }

    private static Evaluator unwrap(Evaluator e) {
        if (e instanceof EvaluatorWrapper) {
            EvaluatorWrapper wrapper = (EvaluatorWrapper) e;
            return unwrap(wrapper.delegate);
        } else {
            return e;
        }
    }

    @Override
    public String toString() {
        return "EW{" +
                "d=" + delegate +
                ", listeners=" + listeners +
                ", hash=" + System.identityHashCode(this) +
                '}';
    }

    @Override
    public EvaluatorWrapper copyOf() {
        return new EvaluatorWrapper(this);
    }

    public void update(Collection<EvaluationListener> listeners) {
        this.listeners = listeners;
        updateActiveEvaluator();
    }

    public Evaluator getDelegate() {
        return delegate;
    }

    public final void setDelegate(Evaluator delegate) {
        this.delegate = delegate;
        updateActiveEvaluator();
    }

    private void updateActiveEvaluator() {
        if (listeners.isEmpty()) {
            this.active = delegate;
        } else {
            this.active = verbose;
        }
    }

    @Override
    public final boolean test(IntToValue intToValue) {
        return active.test(intToValue);
    }

    public final void setStateValues(IntToValue stateValues) {
        this.stateValues = stateValues;
    }

    public final boolean test() {
        return test(this.stateValues);
    }

    @Override
    public FieldReference[] descriptor() {
        return delegate.descriptor();
    }

    @Override
    public final int compare(Evaluator other) {
        return delegate.compare(unwrap(other));
    }
}

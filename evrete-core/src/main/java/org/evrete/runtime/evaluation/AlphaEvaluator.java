package org.evrete.runtime.evaluation;

import org.evrete.api.*;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class AlphaEvaluator implements LogicallyComparable, ValuesPredicate, EvaluationListenerHolder, Copyable<AlphaEvaluator> {
    private final Evaluator delegate;
    private final int uniqueId;
    private final Set<EvaluationListener> listeners = new HashSet<>();
    private int[] valueIndices;
    private final Predicate<IntToValue> mutedPredicate = new Predicate<IntToValue>() {
        @Override
        public boolean test(IntToValue values) {
            return delegate.test(i -> values.apply(valueIndices[i]));
        }
    };

    private final Predicate<IntToValue> verbosePredicate = new Predicate<IntToValue>() {
        @Override
        public boolean test(IntToValue values) {
            IntToValue iv = i -> values.apply(valueIndices[i]);
            boolean b = delegate.test(iv);
            for (EvaluationListener listener : listeners) {
                listener.fire(delegate, iv, b);
            }
            return b;
        }
    };

    private Predicate<IntToValue> activePredicate;

    AlphaEvaluator(int uniqueId, Evaluator e) {
        this.uniqueId = uniqueId;
        this.delegate = e;
        this.remapEvaluator();
    }

    private AlphaEvaluator(AlphaEvaluator other) {
        this.delegate = other.delegate;
        this.uniqueId = other.uniqueId;
        this.valueIndices = other.valueIndices;
        this.activePredicate = other.activePredicate;
        this.listeners.addAll(other.listeners);
        this.remapEvaluator();
    }

    public void remap(int[] indexMapper) {
        this.valueIndices = indexMapper;
        this.remapEvaluator();
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
    public AlphaEvaluator copyOf() {
        return new AlphaEvaluator(this);
    }

    @Override
    public void addListener(EvaluationListener listener) {
        this.listeners.add(listener);
        remapEvaluator();
    }

    @Override
    public void removeListener(EvaluationListener listener) {
        this.listeners.remove(listener);
        remapEvaluator();
    }

    private void remapEvaluator() {
        this.activePredicate = listeners.isEmpty() ? mutedPredicate : verbosePredicate;
    }

    @Override
    public boolean test(IntToValue values) {
        return activePredicate.test(values);
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

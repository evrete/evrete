package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.evaluation.EvaluatorWrapper;
import org.evrete.util.CollectionUtils;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;

class EvaluatorStorageImpl implements Copyable<EvaluatorStorageImpl>, EvaluationListeners {
    private final Map<EvaluatorHandleImpl, EvaluatorWrapper> conditions;
    private final Map<Evaluator, EvaluatorHandleImpl> inverse;
    private final LinkedHashSet<EvaluationListener> listeners;

    EvaluatorStorageImpl() {
        this.conditions = new HashMap<>();
        this.inverse = new HashMap<>();
        this.listeners = new LinkedHashSet<>();
    }

    private EvaluatorStorageImpl(EvaluatorStorageImpl other) {
        this.conditions = new HashMap<>();
        this.inverse = new HashMap<>();
        this.listeners = new LinkedHashSet<>();
        this.listeners.addAll(other.listeners);
        for (Map.Entry<EvaluatorHandleImpl, EvaluatorWrapper> entry : other.conditions.entrySet()) {
            EvaluatorHandleImpl h = entry.getKey();
            EvaluatorWrapper w = entry.getValue();
            this.conditions.put(h, w.copyOf());
            this.inverse.put(w.getDelegate(), h);
        }
    }

    @Override
    public void addListener(EvaluationListener listener) {
        this.listeners.add(listener);
        for (EvaluatorWrapper w : this.conditions.values()) {
            w.update(listeners);
        }
    }

    @Override
    public void removeListener(EvaluationListener listener) {
        this.listeners.remove(listener);
        for (EvaluatorWrapper w : this.conditions.values()) {
            w.update(listeners);
        }
    }

    int compare(EvaluatorHandle h1, EvaluatorHandle h2) {
        EvaluatorWrapper w1 = get(h1, false);
        EvaluatorWrapper w2 = get(h2, false);
        return w1.compare(w2);
    }

    void replace(EvaluatorHandle handle, Evaluator evaluator) {
        EvaluatorWrapper existing = conditions.get((EvaluatorHandleImpl) handle);
        if (existing == null) {
            throw new IllegalArgumentException("Unknown evaluator handle");
        } else {
            if (existing.sameDescriptor(evaluator)) {
                existing.setDelegate(evaluator);
            } else {
                throw new IllegalArgumentException("Mismatched descriptors");
            }
        }
    }

    void replace(EvaluatorHandle handle, ValuesPredicate predicate) {
        EvaluatorWrapper existing = conditions.get((EvaluatorHandleImpl) handle);
        if (existing == null) {
            throw new IllegalArgumentException("Unknown evaluator handle");
        } else {
            final FieldReference[] newDescriptor = CollectionUtils.copyOf(existing.descriptor());
            Evaluator newEvaluator = new Evaluator() {
                @Override
                public FieldReference[] descriptor() {
                    return newDescriptor;
                }

                @Override
                public boolean test(IntToValue t) {
                    return predicate.test(t);
                }
            };
            existing.setDelegate(newEvaluator);
        }
    }

    public EvaluatorWrapper get(EvaluatorHandle handle, boolean returnNull) {
        EvaluatorWrapper w = conditions.get((EvaluatorHandleImpl) Objects.requireNonNull(handle));
        if (w == null) {
            if (returnNull) {
                return null;
            } else {
                throw new IllegalArgumentException("Unknown evaluator " + handle);
            }
        } else {
            return w;
        }
    }


    public EvaluatorHandle save(Evaluator evaluator, double complexity) {
        if (complexity <= 0.0) throw new IllegalArgumentException("Complexity must be positive");

        if (evaluator instanceof EvaluatorWrapper) {
            throw new IllegalArgumentException();
        } else {
            EvaluatorHandleImpl h = inverse.get(evaluator);
            if (h == null) {
                h = new EvaluatorHandleImpl(evaluator, complexity);
                EvaluatorWrapper wrapper = new EvaluatorWrapper(evaluator);
                wrapper.update(listeners);
                this.conditions.put(h, wrapper);
                this.inverse.put(evaluator, h);
            }

            return h;
        }
    }

    @Override
    public String toString() {
        return "Evaluators{" +
                "conditions=" + conditions +
                '}';
    }

    @Override
    public EvaluatorStorageImpl copyOf() {
        return new EvaluatorStorageImpl(this);
    }

    private static class EvaluatorHandleImpl implements EvaluatorHandle {
        private final double complexity;
        private final FieldReference[] descriptor;
        private final String asString;

        EvaluatorHandleImpl(Evaluator evaluator, double complexity) {
            this.descriptor = evaluator.descriptor().clone();
            this.complexity = complexity;
            this.asString = evaluator.toString();
        }

        @Override
        public FieldReference[] descriptor() {
            return descriptor;
        }

        @Override
        public double getComplexity() {
            return complexity;
        }

        @Override
        public String toString() {
            return asString;
        }
    }
}

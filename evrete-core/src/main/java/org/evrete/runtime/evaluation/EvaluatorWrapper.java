package org.evrete.runtime.evaluation;

import org.evrete.api.*;

import java.util.HashSet;
import java.util.Set;

public class EvaluatorWrapper implements Evaluator, Listeners {
    private final Evaluator delegate;
    private final Set<EvaluationListener> listeners = new HashSet<>();
    private final ValuesPredicate verboseUnmapped = new ValuesPredicate() {
        @Override
        public boolean test(IntToValue intToValue) {
            boolean b = delegate.test(intToValue);
            for (EvaluationListener listener : listeners) {
                listener.fire(delegate, intToValue, b);
            }
            return b;
        }
    };
    private final Set<NamedType> namedTypes = new HashSet<>();
    private final ValuesPredicate verboseMapped = new ValuesPredicate() {
        @Override
        public boolean test(IntToValue intToValue) {
            IntToValue mapped = intToValue.remap(indexMapper);
            boolean b = delegate.test(mapped);
            for (EvaluationListener listener : listeners) {
                listener.fire(delegate, mapped, b);
            }
            return b;
        }
    };
    private int[] indexMapper;
    private final ValuesPredicate muteMapped = new ValuesPredicate() {
        @Override
        public boolean test(IntToValue intToValue) {
            IntToValue mapped = intToValue.remap(indexMapper);
            return delegate.test(mapped);
        }
    };
    private ValuesPredicate active;

    public EvaluatorWrapper(Evaluator delegate) {
        this.delegate = unwrap(delegate);
        for (FieldReference ref : delegate.descriptor()) {
            this.namedTypes.add(ref.type());
        }
        updateActiveEvaluator();
    }

    protected EvaluatorWrapper(EvaluatorWrapper other) {
        this.delegate = unwrap(other.delegate);
        this.listeners.addAll(other.listeners);
        this.namedTypes.addAll(other.namedTypes);
        this.indexMapper = other.indexMapper;
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

    public final void remap(int[] indexMapper) {
        this.indexMapper = indexMapper;
        updateActiveEvaluator();
    }

    @Override
    public final void addListener(EvaluationListener listener) {
        this.listeners.add(listener);
        updateActiveEvaluator();
    }

    @Override
    public final void removeListener(EvaluationListener listener) {
        this.listeners.remove(listener);
        updateActiveEvaluator();
    }

    public Set<NamedType> getNamedTypes() {
        return namedTypes;
    }

    private void updateActiveEvaluator() {
        if (listeners.isEmpty()) {
            if (indexMapper == null) {
                this.active = delegate;
            } else {
                this.active = muteMapped;
            }
        } else {
            if (indexMapper == null) {
                this.active = verboseUnmapped;
            } else {
                this.active = verboseMapped;
            }
        }
    }

    @Override
    public final boolean test(IntToValue intToValue) {
        return active.test(intToValue);
    }

    @Override
    public final double getComplexity() {
        return delegate.getComplexity();
    }

    @Override
    public FieldReference[] descriptor() {
        return delegate.descriptor();
    }

    @Override
    public final int compare(LogicallyComparable other) {
        // TODO check instances
        return delegate.compare(other);
    }
}

package org.evrete.runtime;

import org.evrete.api.ValueHandle;
import org.evrete.api.ValueResolver;
import org.evrete.runtime.evaluation.AlphaEvaluator;
import org.evrete.runtime.evaluation.EvaluatorWrapper;

class RuntimeAlphaEvaluator {
    private final EvaluatorWrapper delegate;
    private final ValueResolver resolver;
    private final ActiveField[] activeDescriptor;
    private final int index;

    RuntimeAlphaEvaluator(AlphaEvaluator alphaEvaluator, Evaluators evaluators, ValueResolver resolver) {
        this.delegate = evaluators.get(alphaEvaluator.getDelegate());
        this.activeDescriptor = alphaEvaluator.getDescriptor();
        this.index = alphaEvaluator.getIndex();
        this.resolver = resolver;
    }

    int getIndex() {
        return index;
    }

    public boolean test(ValueHandle[] values) {
        return delegate.test(i -> resolver.getValue(values[activeDescriptor[i].getValueIndex()]));
    }

}

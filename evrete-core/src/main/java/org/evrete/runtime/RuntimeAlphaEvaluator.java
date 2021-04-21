package org.evrete.runtime;

import org.evrete.api.ActiveField;
import org.evrete.api.FieldToValueHandle;
import org.evrete.api.ValueResolver;
import org.evrete.runtime.evaluation.AlphaEvaluator;
import org.evrete.runtime.evaluation.EvaluatorWrapper;

class RuntimeAlphaEvaluator {
    private final EvaluatorWrapper delegate;
    private final ActiveField[] activeDescriptor;
    private final int index;

    RuntimeAlphaEvaluator(AlphaEvaluator alphaEvaluator, Evaluators evaluators) {
        this.delegate = evaluators.get(alphaEvaluator.getDelegate());
        this.activeDescriptor = alphaEvaluator.getActiveDescriptor();
        this.index = alphaEvaluator.getIndex();
    }

    int getIndex() {
        return index;
    }


    public boolean test(ValueResolver resolver, FieldToValueHandle values) {
        return delegate.test(i -> resolver.getValue(values.apply(activeDescriptor[i])));
    }

}

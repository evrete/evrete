package org.evrete.runtime;

import org.evrete.api.EvaluatorHandle;
import org.evrete.runtime.evaluation.BetaEvaluator;
import org.evrete.runtime.evaluation.EvaluatorWrapper;

class RuntimeBetaEvaluator {
    private final EvaluatorWrapper[] constituents;

    RuntimeBetaEvaluator(AbstractRuntime<?, ?> ctx, BetaEvaluator evaluator) {
        EvaluatorHandle[] handles = evaluator.constituents();
        this.constituents = new EvaluatorWrapper[handles.length];
        for (int i = 0; i < handles.length; i++) {
            this.constituents[i] = ctx.getEvaluatorWrapper(handles[i], false);
        }
    }

    public boolean test() {
        for (EvaluatorWrapper w : constituents) {
            if (!w.test()) return false;
        }
        return true;
    }

    EvaluatorWrapper[] constituents() {
        return constituents;
    }

}

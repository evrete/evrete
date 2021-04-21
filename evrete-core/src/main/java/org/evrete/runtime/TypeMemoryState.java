package org.evrete.runtime;

import org.evrete.api.ActiveField;
import org.evrete.api.Type;
import org.evrete.runtime.evaluation.AlphaEvaluator;

/**
 * A state instance to be used in memory initialization and hot deployment updates
 */
class TypeMemoryState {
    final Type<?> type;
    final ActiveField[] activeFields;
    final RuntimeAlphaEvaluator[] alphaEvaluators1;

    TypeMemoryState(Type<?> type, ActiveField[] activeFields, Evaluators evaluators, AlphaEvaluator[] alphaEvaluators) {
        this.type = type;
        this.activeFields = activeFields;
        this.alphaEvaluators1 = new RuntimeAlphaEvaluator[alphaEvaluators.length];

        for (int i = 0; i < alphaEvaluators.length; i++) {
            this.alphaEvaluators1[i] = new RuntimeAlphaEvaluator(alphaEvaluators[i], evaluators);
        }
    }
}

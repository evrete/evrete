package org.evrete.runtime;

import org.evrete.api.ActiveField;
import org.evrete.api.Type;
import org.evrete.runtime.evaluation.AlphaEvaluator;

/**
 * A state instance to be used in memory initialization and hot deployment updates
 */
class TypeMemoryState {
    Type<?> type;
    ActiveField[] activeFields;
    AlphaEvaluator[] alphaEvaluators;

    TypeMemoryState(Type<?> type, ActiveField[] activeFields, AlphaEvaluator[] alphaEvaluators) {
        this.type = type;
        this.activeFields = activeFields;
        this.alphaEvaluators = alphaEvaluators;
    }
}

package org.evrete.runtime;

import org.evrete.api.Type;
import org.evrete.api.TypeField;
import org.evrete.runtime.evaluation.AlphaEvaluator;

/**
 * A state instance to be used in memory initialization and hot deployment updates
 */
class TypeMemoryState {
    private final Type<?> type;
    final TypeField[] fields;
    final RuntimeAlphaEvaluator[] alphaEvaluators;

    TypeMemoryState(Type<?> type, ActiveField[] activeFields, Evaluators evaluators, AlphaEvaluator[] alphaEvaluators) {
        this.type = type;
        this.alphaEvaluators = new RuntimeAlphaEvaluator[alphaEvaluators.length];
        this.fields = new TypeField[activeFields.length];
        for (int i = 0; i < activeFields.length; i++) {
            int fieldId = activeFields[i].field();
            TypeField field = type.getField(fieldId);
            this.fields[i] = field;
        }

        for (int i = 0; i < alphaEvaluators.length; i++) {
            this.alphaEvaluators[i] = new RuntimeAlphaEvaluator(alphaEvaluators[i], evaluators);
        }
    }
}

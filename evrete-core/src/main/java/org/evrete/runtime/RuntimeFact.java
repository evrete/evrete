package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.evaluation.AlphaEvaluator;
import org.evrete.util.Bits;

import java.util.Arrays;
import java.util.Objects;
//TODO !!!! create one-field implementation

/**
 * A runtime representation of a fact, ready for insert operation
 */
class RuntimeFact {
    private static final Bits EMPTY = new Bits();
    final Object[] fieldValues;
    final ValueHandle[] valueHandles;
    final FactHandleVersioned factHandle;
    final Bits alphaTests;
    final ValueResolver resolver;

    RuntimeFact() {
        this.fieldValues = null;
        this.factHandle = null;
        this.alphaTests = null;
        this.valueHandles = null;
        this.resolver = null;
    }

    RuntimeFact(ValueResolver resolver, TypeMemoryState typeMemoryState, FactHandleVersioned factHandle, FactRecord factRecord) {
        this.factHandle = factHandle;
        this.resolver = resolver;
        ActiveField[] activeFields = typeMemoryState.activeFields;
        this.fieldValues = new Object[activeFields.length];
        this.valueHandles = new ValueHandle[activeFields.length];
        for (int i = 0; i < fieldValues.length; i++) {
            this.fieldValues[i] = activeFields[i].readValue(factRecord.instance);
        }
        AlphaEvaluator[] alphaEvaluators = typeMemoryState.alphaEvaluators;
        if (alphaEvaluators.length == 0) {
            this.alphaTests = EMPTY;
        } else {
            FieldToValue func = field -> fieldValues[field.getValueIndex()];
            this.alphaTests = new Bits();
            for (AlphaEvaluator alphaEvaluator : alphaEvaluators) {
                if (alphaEvaluator.test(func)) {
                    this.alphaTests.set(alphaEvaluator.getIndex());
                }
            }
        }
    }

    ValueHandle getValue(ActiveField field) {
        int idx = field.getValueIndex();
        ValueHandle h = this.valueHandles[idx];
        if (h == null) {
            synchronized (valueHandles) {
                h = this.valueHandles[idx];
                if (h == null) {
                    h = resolver.getValueHandle(field.getValueType(), fieldValues[idx]);
                    valueHandles[idx] = h;
                }
            }
        }
        return h;
    }

    boolean sameValues(RuntimeFact other) {
        if (other == null) return false;
        for (int i = 0; i < fieldValues.length; i++) {
            if (!Objects.equals(fieldValues[i], other.fieldValues[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "{handle=" + factHandle.getHandle() +
                ", values=" + Arrays.toString(fieldValues) +
                '}';
    }
}

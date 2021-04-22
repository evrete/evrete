package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.TypeField;
import org.evrete.api.ValueHandle;
import org.evrete.api.ValueResolver;
import org.evrete.util.Bits;

import java.util.Arrays;
import java.util.Objects;
//TODO create one-field implementation

/**
 * A runtime representation of a fact, ready for insert operation
 */
class RuntimeFact {
    private static final Bits EMPTY = new Bits();
    private final ValueHandle[] valueHandles;
    final FactHandleVersioned factHandle;
    final Bits alphaTests;

    RuntimeFact() {
        this.factHandle = null;
        this.alphaTests = null;
        this.valueHandles = null;
    }

    RuntimeFact(ValueResolver resolver, TypeMemoryState typeMemoryState, FactHandleVersioned factHandle, FactRecord factRecord) {
        this.factHandle = factHandle;
        TypeField[] fields = typeMemoryState.fields;
        this.valueHandles = new ValueHandle[fields.length];
        for (int i = 0; i < valueHandles.length; i++) {
            TypeField f = fields[i];
            this.valueHandles[i] = resolver.getValueHandle(f.getValueType(), f.readValue(factRecord.instance));
        }

        RuntimeAlphaEvaluator[] alphaEvaluators = typeMemoryState.alphaEvaluators;
        if (alphaEvaluators.length == 0) {
            this.alphaTests = EMPTY;
        } else {
            FieldToValueHandle funcOld = field -> valueHandles[field.getValueIndex()];
            //IntToValueHandle func = i -> valueHandles[field.getValueIndex()];
            this.alphaTests = new Bits();
            for (RuntimeAlphaEvaluator alphaEvaluator : alphaEvaluators) {
                if (alphaEvaluator.test(resolver, funcOld)) {
                    this.alphaTests.set(alphaEvaluator.getIndex());
                }
            }
        }
    }

    ValueHandle getValue(ActiveField field) {
        return this.valueHandles[field.getValueIndex()];
    }

    boolean sameValues(RuntimeFact other) {
        if (other == null) return false;
        for (int i = 0; i < valueHandles.length; i++) {
            if (!Objects.equals(valueHandles[i], other.valueHandles[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "{handle=" + factHandle.getHandle() +
                ", values=" + Arrays.toString(valueHandles) +
                '}';
    }
}

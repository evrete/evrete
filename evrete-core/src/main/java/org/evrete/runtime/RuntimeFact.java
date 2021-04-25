package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.TypeField;
import org.evrete.api.ValueHandle;
import org.evrete.util.Bits;

import java.util.Arrays;
import java.util.Objects;
//TODO create a one-field implementation

/**
 * A runtime representation of a fact, ready for insert operation
 */
class RuntimeFact {
    // A convenience fact instance that is never equal to others
    static final RuntimeFact DUMMY_FACT = new RuntimeFact() {
        @Override
        final boolean sameValues(RuntimeFact other) {
            return false;
        }
    };
    private static final Bits EMPTY = new Bits();
    private final ValueHandle[] valueHandles;
    final FactHandleVersioned factHandle;
    final Bits alphaTests;

    private RuntimeFact() {
        this.factHandle = null;
        this.alphaTests = null;
        this.valueHandles = null;
    }

    RuntimeFact(TypeMemoryState state, FactHandleVersioned factHandle, FactRecord factRecord) {
        this.factHandle = factHandle;
        TypeField[] fields = state.fields;
        this.valueHandles = new ValueHandle[fields.length];
        for (int i = 0; i < valueHandles.length; i++) {
            TypeField f = fields[i];
            this.valueHandles[i] = state.resolver.getValueHandle(f.getValueType(), f.readValue(factRecord.instance));
        }

        if (state.alphaEvaluators.length == 0) {
            this.alphaTests = EMPTY;
        } else {
            this.alphaTests = new Bits();
            for (RuntimeAlphaEvaluator alphaEvaluator : state.alphaEvaluators) {
                if (alphaEvaluator.test(valueHandles)) {
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

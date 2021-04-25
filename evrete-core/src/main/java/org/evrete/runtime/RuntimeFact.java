package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.ValueHandle;
import org.evrete.util.Bits;

import java.util.Arrays;
import java.util.Objects;

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
        this.alphaTests = EMPTY;
        this.valueHandles = new ValueHandle[0];
    }

    RuntimeFact(FactHandleVersioned factHandle, ValueHandle[] valueHandles, Bits alphaTests) {
        this.valueHandles = valueHandles;
        this.factHandle = factHandle;
        this.alphaTests = alphaTests;
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
        return "{handle=" + factHandle +
                ", values=" + Arrays.toString(valueHandles) +
                '}';
    }
}

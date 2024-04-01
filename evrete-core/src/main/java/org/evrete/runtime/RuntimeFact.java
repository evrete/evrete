package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.FieldValue;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;

/**
 * A runtime representation of a fact, ready for insert operation
 */
public class RuntimeFact {
    // A convenience fact instance that is never equal to others
    static final RuntimeFact DUMMY_FACT = new RuntimeFact() {
        @Override
        boolean sameValues(RuntimeFact other) {
            return false;
        }
    };
    private static final BitSet EMPTY = new BitSet();
    public final FactHandleVersioned factHandle;
    public final FactRecord factRecord;
    final BitSet alphaTests;
    private final FieldValue[] fieldValues;

    private RuntimeFact() {
        this.factHandle = null;
        this.alphaTests = EMPTY;
        this.fieldValues = new FieldValue[0];
        this.factRecord = null;
    }

    RuntimeFact(FactRecord factRecord, FactHandleVersioned factHandle, FieldValue[] fieldValues, BitSet alphaTests) {
        this.fieldValues = fieldValues;
        this.factHandle = factHandle;
        this.alphaTests = alphaTests;
        this.factRecord = factRecord;
    }

    FieldValue getValue(ActiveField field) {
        return this.fieldValues[field.getValueIndex()];
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
        return "{handle=" + factHandle +
                ", values=" + Arrays.toString(fieldValues) +
                '}';
    }
}

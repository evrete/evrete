package org.evrete.runtime;

import org.evrete.api.ActiveField;
import org.evrete.api.FactHandleVersioned;
import org.evrete.api.KeyedFactStorage;
import org.evrete.api.ValueHandle;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.Collection;

class KeyMemoryBucketAlpha extends KeyMemoryBucket {
    private final AlphaBucketMeta alphaMask;

    KeyMemoryBucketAlpha(MemoryComponent runtime, FieldsKey typeFields, AlphaBucketMeta alphaMask) {
        super(runtime, typeFields);
        this.alphaMask = alphaMask;
    }

    @Override
    final void insert(Iterable<RuntimeFact> facts) {
        current = DUMMY_FACT;
        for (RuntimeFact fact : facts) {
            if (alphaMask.test(fact.alphaTests)) {
                if (current.sameValues(fact)) {
                    insertData.add(fact.factHandle);
                } else {
                    // Key changed, ready for batch insert
                    flushBuffer();
                    insertData.add(fact.factHandle);
                    current = fact;
                }
            }
        }

        if (!insertData.isEmpty()) {
            flushBuffer();
        }
    }

    @Override
    final void flushBuffer() {
        if (current != DUMMY_FACT) {
            Helper helper = buildKeyAndHash();
            helper.do1(fieldData, insertData);
            insertData.clear();
        }
    }

    private Helper buildKeyAndHash() {
        ValueHandle[] valueHandles = new ValueHandle[activeFields.length];
        for (int i = 0; i < activeFields.length; i++) {
            ActiveField field = activeFields[i];
            Object v = current.fieldValues[field.getValueIndex()];
            ValueHandle valueHandle = valueResolver.getValueHandle(field.getValueType(), v);
            valueHandles[i] = valueHandle;
        }
        return new Helper(valueHandles);
    }

    private static class Helper {
        ValueHandle[] valueHandles;

        Helper(ValueHandle[] valueHandles) {
            this.valueHandles = valueHandles;
        }

        void do1(KeyedFactStorage memory, Collection<FactHandleVersioned> facts) {
            for (ValueHandle valueHandle : valueHandles) {
                memory.write(valueHandle);
            }
            memory.write(facts);
        }
    }
}

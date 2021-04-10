package org.evrete.runtime;

import org.evrete.api.ActiveField;
import org.evrete.api.FactHandleVersioned;
import org.evrete.api.KeyedFactStorage;
import org.evrete.api.ValueHandle;

import java.util.Collection;

class KeyMemoryBucketNoAlpha extends KeyMemoryBucket {

    KeyMemoryBucketNoAlpha(MemoryComponent runtime, FieldsKey typeFields) {
        super(runtime, typeFields);
    }

    @Override
    void insert(Iterable<RuntimeFact> facts) {
        current = null;
        for (RuntimeFact fact : facts) {
            if (current == null) {
                insertData.add(fact.factHandle);
                current = fact;
            } else {
                if (fact.sameValues(current)) {
                    insertData.add(fact.factHandle);
                } else {
                    // Key changed, ready for batch insert
                    doStuff();
                    insertData.add(fact.factHandle);
                    current = fact;
                }
            }
        }

        if (!insertData.isEmpty()) {
            doStuff();
        }
    }

    //TODO !!!! rename & refactor
    void doStuff() {
        Helper helper = buildKeyAndHash(current);
        helper.do1(fieldData, insertData);
        insertData.clear();
    }

    private Helper buildKeyAndHash(RuntimeFact fact) {
        ValueHandle[] valueHandles = new ValueHandle[activeFields.length];
        for (int i = 0; i < activeFields.length; i++) {
            ActiveField field = activeFields[i];
            Object v = fact.fieldValues[field.getValueIndex()];
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

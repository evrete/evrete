package org.evrete.runtime;

import org.evrete.api.ActiveField;
import org.evrete.api.FactHandleVersioned;
import org.evrete.api.KeyedFactStorage;
import org.evrete.api.ValueHandle;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.Collection;
import java.util.LinkedList;

//TODO !!! create implementation for zero, one, and multiple fields
class KeyMemoryBucket extends MemoryComponent {
    private final KeyedFactStorage fieldData;
    private final AlphaBucketMeta alphaMask;
    private final ActiveField[] activeFields;
    private final Collection<FactHandleVersioned> insertData = new LinkedList<>();


    KeyMemoryBucket(MemoryComponent runtime, FieldsKey typeFields, AlphaBucketMeta alphaMask) {
        super(runtime);
        this.alphaMask = alphaMask;
        this.fieldData = memoryFactory.newBetaStorage(typeFields.getFields());
        this.activeFields = typeFields.getFields();
    }

    void insert(Iterable<RuntimeFact> facts) {
        RuntimeFact current = null;
        for (RuntimeFact fact : facts) {
            if (alphaMask.test(fact.alphaTests)) {
                if (current == null) {
                    insertData.add(fact.factHandle);
                    current = fact;
                } else {
                    if (fact.sameValues(current)) {
                        insertData.add(fact.factHandle);
                    } else {
                        // Key changed, ready for batch insert
                        doStuff(current);

                        insertData.add(fact.factHandle);
                        current = fact;
                    }
                }
            }
        }

        if (!insertData.isEmpty()) {
            doStuff(current);
        }


    }

    //TODO !!!! rename & refactor
    private void doStuff(RuntimeFact current) {
        Helper helper = buildKeyAndHash(current);
        helper.do1(fieldData, insertData);
        insertData.clear();
    }

    @Override
    protected void clearLocalData() {
        fieldData.clear();
    }

    KeyedFactStorage getFieldData() {
        return fieldData;
    }

    @Override
    public void commitChanges() {
        fieldData.commitChanges();
    }

    @Override
    public String toString() {
        return fieldData.toString();
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

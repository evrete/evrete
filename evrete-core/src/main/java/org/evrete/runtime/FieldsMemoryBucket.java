package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.Collection;
import java.util.LinkedList;

class FieldsMemoryBucket extends MemoryComponent {
    private final KeyedFactStorage fieldData;
    private final AlphaBucketMeta alphaMask;
    private final ActiveField[] activeFields;


    FieldsMemoryBucket(MemoryComponent runtime, FieldsKey typeFields, AlphaBucketMeta alphaMask) {
        super(runtime);
        this.alphaMask = alphaMask;
        this.fieldData = memoryFactory.newBetaStorage(typeFields.getFields());
        this.activeFields = typeFields.getFields();
    }

    void insert(Iterable<RuntimeFact> facts) {
        RuntimeFact current = null;
        Collection<FactHandleVersioned> insertData = new LinkedList<>();
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
                        Helper helper = buildKeyAndHash(current);
                        fieldData.insert(helper.key, helper.hash, insertData);
                        insertData.clear();
                        insertData.add(fact.factHandle);
                        current = fact;
                    }
                }
            }
        }

        if (!insertData.isEmpty()) {
            Helper helper = buildKeyAndHash(current);
            fieldData.insert(helper.key, helper.hash, insertData);
        }


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
        int hash = 0;
        for (int i = 0; i < activeFields.length; i++) {
            ActiveField field = activeFields[i];
            Object v = fact.fieldValues[field.getValueIndex()];
            ValueHandle valueHandle = valueResolver.getValueHandle(field.getValueType(), v);
            hash += valueHandle.hashCode() * 37;
            valueHandles[i] = valueHandle;
        }
        IntToValueHandle key = i -> valueHandles[i];
        return new Helper(key, hash);
    }

    private static class Helper {
        final IntToValueHandle key;
        final int hash;

        Helper(IntToValueHandle key, int hash) {
            this.key = key;
            this.hash = hash;
        }
    }
}

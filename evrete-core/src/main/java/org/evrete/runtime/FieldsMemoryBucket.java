package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.util.Bits;

import java.util.Collection;
import java.util.LinkedList;

class FieldsMemoryBucket extends MemoryComponent {
    private final KeyedFactStorage fieldData;
    private final AlphaBucketMeta alphaMask;

    FieldsMemoryBucket(MemoryComponent runtime, FieldsKey typeFields, AlphaBucketMeta alphaMask) {
        super(runtime);
        this.alphaMask = alphaMask;
        this.fieldData = memoryFactory.newBetaStorage(typeFields.getFields());
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
    void insert(LazyValues values, Bits alphaTests, FactHandleVersioned handle) {
        throw new UnsupportedOperationException();
/*
        if (alphaMask.test(alphaTests)) {
            fieldData.insert(values.getValues(), values.keyHash(), handle);
        }
*/
    }

    @Override
    public String toString() {
        return fieldData.toString();
    }

    private Helper buildKeyAndHash(RuntimeFact fact) {
        ValueHandle[] valueHandles = new ValueHandle[fact.fieldValues.length];
        int hash = 0;
        for (int i = 0; i < valueHandles.length; i++) {
            ActiveField field = fact.activeFields[i];
            Object v = fact.fieldValues[i];
            ValueHandle valueHandle = valueResolver.getValueHandle(field.getValueType(), v);
            hash += valueHandle.hashCode() * 37;
            valueHandles[i] = valueHandle;
        }
        FieldToValueHandle key = new FieldToValueHandle() {
            @Override
            public ValueHandle apply(ActiveField field) {
                return valueHandles[field.getValueIndex()];
            }
        };
        return new Helper(key, hash);
    }

    private class Helper {
        final FieldToValueHandle key;
        final int hash;

        Helper(FieldToValueHandle key, int hash) {
            this.key = key;
            this.hash = hash;
        }
    }
}

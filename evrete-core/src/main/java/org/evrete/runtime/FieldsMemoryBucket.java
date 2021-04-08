package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.KeyedFactStorage;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.util.Bits;

class FieldsMemoryBucket extends MemoryComponent {
    private final KeyedFactStorage fieldData;
    private final AlphaBucketMeta alphaMask;

    FieldsMemoryBucket(MemoryComponent runtime, FieldsKey typeFields, AlphaBucketMeta alphaMask) {
        super(runtime);
        this.alphaMask = alphaMask;
        this.fieldData = memoryFactory.newBetaStorage(typeFields.getFields());
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
        if (alphaMask.test(alphaTests)) {
            fieldData.insert(values.getValues(), values.keyHash(), handle);
        }
    }

    @Override
    public String toString() {
        return fieldData.toString();
    }
}

package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.FieldToValueHandle;
import org.evrete.api.SharedBetaFactStorage;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.util.Bits;

class FieldsMemoryBucket extends MemoryComponent {
    private final SharedBetaFactStorage fieldData;
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

    SharedBetaFactStorage getFieldData() {
        return fieldData;
    }

    @Override
    public void commitChanges() {
        fieldData.commitChanges();
    }

    @Override
    void insert(FieldToValueHandle key, Bits alphaTests, FactHandleVersioned value) {
        if (alphaMask.test(alphaTests)) {
            fieldData.insert(key, value);
        }
    }

    @Override
    public String toString() {
        return fieldData.toString();
    }
}

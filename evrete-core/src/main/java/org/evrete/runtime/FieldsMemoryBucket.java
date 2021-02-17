package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.FieldToValueHandle;
import org.evrete.api.SharedBetaFactStorage;
import org.evrete.api.spi.InnerFactMemory;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.function.Consumer;

class FieldsMemoryBucket extends MemoryComponent implements InnerFactMemory {
    private final SharedBetaFactStorage fieldData;
    private final AlphaBucketMeta alphaMask;

    FieldsMemoryBucket(MemoryComponent runtime, FieldsKey typeFields, AlphaBucketMeta alphaMask) {
        super(runtime);
        this.alphaMask = alphaMask;
        this.fieldData = memoryFactory.newBetaStorage(typeFields);
    }

    @Override
    protected void forEachChildComponent(Consumer<MemoryComponent> consumer) {
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
    public void insert(FactHandleVersioned value, FieldToValueHandle key) {
        if (alphaMask.test(memoryFactory.getValueResolver(), key)) {
            fieldData.insert(value, key);
        }
    }

    @Override
    public String toString() {
        return fieldData.toString();
    }
}

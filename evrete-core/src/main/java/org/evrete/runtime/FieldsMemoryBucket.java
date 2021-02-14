package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.FieldToValue;
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

    void insert(FactHandleVersioned handle, FieldToValue values, boolean[] alphaTests) {
        if (alphaMask.test(alphaTests)) {
            fieldData.insert(handle, values);
        }
    }

    @Override
    public void insert(FactHandleVersioned fact, FieldToValue values) {
        if (alphaMask.test(values)) {
            fieldData.insert(fact, values);
        }
    }

    /*
    void delete(ReIterable<? extends RuntimeFact> facts) {
        ReIterator<? extends RuntimeFact> it = facts.iterator();

        while (it.hasNext()) {
            delete(it.next());
        }
    }

    void delete(RuntimeFact fact) {
        if (alphaMask.test(fact)) {
            fieldData.delete(fact);
        }
    }
*/

    @Override
    public String toString() {
        return fieldData.toString();
    }
}

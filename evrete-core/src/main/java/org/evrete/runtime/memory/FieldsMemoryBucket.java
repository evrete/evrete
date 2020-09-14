package org.evrete.runtime.memory;

import org.evrete.api.FieldsKey;
import org.evrete.api.Memory;
import org.evrete.api.RuntimeFact;
import org.evrete.api.SharedBetaFactStorage;
import org.evrete.runtime.RuntimeObject;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.Collection;

class FieldsMemoryBucket implements Memory {
    private final SharedBetaFactStorage fieldData;
    private final AlphaBucketMeta alphaMask;

    FieldsMemoryBucket(SessionMemory runtime, FieldsKey typeFields, AlphaBucketMeta alphaMask) {
        this.alphaMask = alphaMask;
        this.fieldData = runtime.newSharedKeyStorage(typeFields);
    }

    public void clear() {
        fieldData.clear();
    }

    SharedBetaFactStorage getFieldData() {
        return fieldData;
    }

    @Override
    public void commitChanges() {
        fieldData.commitChanges();
    }

    void insert(Collection<RuntimeObject> facts) {
        fieldData.insert(facts, alphaMask);
    }

    void delete(Collection<RuntimeFact> facts) {
        fieldData.delete(facts, alphaMask);
    }
}

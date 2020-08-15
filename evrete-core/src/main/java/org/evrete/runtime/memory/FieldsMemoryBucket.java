package org.evrete.runtime.memory;

import org.evrete.api.FieldsKey;
import org.evrete.api.RuntimeFact;
import org.evrete.api.ValueRow;
import org.evrete.api.spi.CollectionsService;
import org.evrete.api.spi.SharedBetaFactStorage;
import org.evrete.runtime.AlphaBucketData;
import org.evrete.runtime.RuntimeFactType;
import org.evrete.runtime.RuntimeObject;

import java.util.Collection;

class FieldsMemoryBucket {
    private final SharedBetaFactStorage fieldData;
    private final AlphaBucketData alphaMask;
    private final int bucketIndex;

    private RuntimeFactType[] factTypesByAlpha;
    private boolean deltaAvailable = false;

    FieldsMemoryBucket(SessionMemory runtime, FieldsKey typeFields, AlphaBucketData alphaMask) {
        CollectionsService collectionsService = runtime.getConfiguration().getCollectionsService();
        this.alphaMask = alphaMask;
        this.fieldData = collectionsService.newBetaStorage(typeFields);
        this.bucketIndex = alphaMask.getBucketIndex();
    }

    public void clear() {
        fieldData.clear();
    }

    public SharedBetaFactStorage getFieldData() {
        return fieldData;
    }

    void insert(Collection<RuntimeObject> facts) {
        fieldData.ensureExtraCapacity(facts.size());
        for (RuntimeObject fact : facts) {
            if (alphaMask.test(fact)) {
                if (fieldData.insert(fact)) {
                    deltaAvailable = true;
                    for (RuntimeFactType type : this.factTypesByAlpha) {
                        type.markInsertDeltaAvailable();
                    }
                }
            }
        }
    }


    void retract(Collection<RuntimeFact> facts) {
        for (RuntimeFact fact : facts) {
            ValueRow lastObjectKey;
            if (alphaMask.test(fact)) {
                if ((lastObjectKey = fieldData.delete(fact)) != null) {
                    for (RuntimeFactType type : this.factTypesByAlpha) {
                        type.addToDeleteKey(lastObjectKey);
                    }
                }
            }
        }
    }

    void mergeDelta() {
        if (deltaAvailable) {
            fieldData.mergeDelta();
            deltaAvailable = false;
        }
    }

    public int getBucketIndex() {
        return bucketIndex;
    }

    void setFactTypesByAlpha(RuntimeFactType[] factTypesByAlpha) {
        this.factTypesByAlpha = factTypesByAlpha;
    }
}

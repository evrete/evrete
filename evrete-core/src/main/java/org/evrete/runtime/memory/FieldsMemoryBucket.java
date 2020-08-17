package org.evrete.runtime.memory;

import org.evrete.api.FieldsKey;
import org.evrete.api.RuntimeFact;
import org.evrete.api.spi.CollectionsService;
import org.evrete.api.spi.SharedBetaFactStorage;
import org.evrete.runtime.AlphaBucketMeta;
import org.evrete.runtime.RuntimeObject;

import java.util.Collection;

class FieldsMemoryBucket {
    private final SharedBetaFactStorage fieldData;
    private final AlphaBucketMeta alphaMask;
    private final int bucketIndex;

    private boolean insertDeltaAvailable = false;
    private boolean deleteDeltaAvailable = false;

    FieldsMemoryBucket(SessionMemory runtime, FieldsKey typeFields, AlphaBucketMeta alphaMask) {
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
            insertSingle(fact);
        }
    }

    void insertSingle(RuntimeObject rto) {
        if (alphaMask.test(rto)) {
            if (fieldData.insert(rto)) {
                insertDeltaAvailable = true;
            }
        }
    }


    void retract(Collection<RuntimeFact> facts) {
        for (RuntimeFact fact : facts) {
            if (alphaMask.test(fact)) {
                if (fieldData.delete(fact)) {
                    deleteDeltaAvailable = true;
                }
            }
        }
    }

    void mergeInsertDelta() {
        if (insertDeltaAvailable) {
            fieldData.mergeDelta();
            insertDeltaAvailable = false;
        }
    }

    void mergeDeleteDelta() {
        if (deleteDeltaAvailable) {
            fieldData.clearDeletedKeys();
            deleteDeltaAvailable = false;
        }
    }

    public int getBucketIndex() {
        return bucketIndex;
    }

}

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

    //TODO !!! delete directly with a predicate
    void retract(Collection<RuntimeFact> facts) {
        for (RuntimeFact fact : facts) {
            if (alphaMask.test(fact)) {
                if (fieldData.delete(fact)) {
                    //deleteDeltaAvailable = true;
                }
            }
        }
    }

/*
    void mergeInsertDelta() {
        //if (insertDeltaAvailable) {
            fieldData.mergeDelta();
            //insertDeltaAvailable = false;
        //}
    }

    void mergeDeleteDelta() {
        //if (deleteDeltaAvailable) {
            fieldData.clearDeletedKeys();
            //deleteDeltaAvailable = false;
        //}
    }
*/
}

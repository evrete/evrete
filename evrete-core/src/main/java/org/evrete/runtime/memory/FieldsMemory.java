package org.evrete.runtime.memory;

import org.evrete.api.FieldsKey;
import org.evrete.api.Memory;
import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;
import org.evrete.api.SharedBetaFactStorage;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.RuntimeObject;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.Collection;

public class FieldsMemory implements Memory {
    private final FieldsKey typeFields;
    private final SessionMemory runtime;
    private final ArrayOf<FieldsMemoryBucket> alphaBuckets;

    FieldsMemory(SessionMemory runtime, FieldsKey typeFields) {
        this.runtime = runtime;
        this.typeFields = typeFields;
        this.alphaBuckets = new ArrayOf<>(FieldsMemoryBucket.class);
    }

    public SharedBetaFactStorage get(AlphaBucketMeta mask) {
        int bucketIndex = mask.getBucketIndex();
        if (bucketIndex >= alphaBuckets.data.length) {
            throw new IllegalArgumentException("No alpha bucket created for " + mask);
        } else {
            SharedBetaFactStorage storage = alphaBuckets.data[bucketIndex].getFieldData();
            if (storage == null) {
                throw new IllegalArgumentException("No alpha bucket created for " + mask);
            } else {
                return storage;
            }
        }
    }

    @Override
    public void commitChanges() {
        for(FieldsMemoryBucket bucket : alphaBuckets.data) {
            bucket.commitChanges();
        }
    }

    FieldsMemoryBucket touchMemory(AlphaBucketMeta alphaMeta) {
        int bucketIndex = alphaMeta.getBucketIndex();
        if (alphaBuckets.isEmptyAt(bucketIndex)) {
            FieldsMemoryBucket newBucket = new FieldsMemoryBucket(runtime, typeFields, alphaMeta);
            alphaBuckets.set(bucketIndex, newBucket);
            return newBucket;
        }
        return null;
    }

    void onNewAlphaBucket(AlphaBucketMeta alphaMeta, ReIterator<RuntimeObject> existingFacts) {
        FieldsMemoryBucket newBucket = touchMemory(alphaMeta);
        assert newBucket != null;
        SharedBetaFactStorage betaFactStorage = newBucket.getFieldData();
        if (existingFacts.reset() > 0) {
            while (existingFacts.hasNext()) {
                RuntimeObject rto = existingFacts.next();
                if (alphaMeta.test(rto)) {
                    throw new UnsupportedOperationException();
                    //betaFactStorage.insertDirect(rto);
                }
            }
        }
    }

    void clear() {
        for (FieldsMemoryBucket bucket : alphaBuckets.data) {
            bucket.clear();
        }
    }

    void insert(Collection<RuntimeObject> facts) {
        for (FieldsMemoryBucket bucket : alphaBuckets.data) {
            bucket.insert(facts);
        }
    }

    void retract(Collection<RuntimeFact> facts) {
        for (FieldsMemoryBucket bucket : alphaBuckets.data) {
            bucket.retract(facts);
        }
    }
}

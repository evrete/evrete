package org.evrete.runtime.memory;

import org.evrete.api.FieldsKey;
import org.evrete.api.RuntimeFact;
import org.evrete.api.spi.SharedBetaFactStorage;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.*;

import java.util.Collection;

public class FieldsMemory implements MemoryChangeListener {
    private final FieldsKey typeFields;
    private final SessionMemory runtime;
    private final ArrayOf<FieldsMemoryBucket> alphaBuckets;

    FieldsMemory(SessionMemory runtime, FieldsKey typeFields) {
        this.runtime = runtime;
        this.typeFields = typeFields;
        this.alphaBuckets = new ArrayOf<>(FieldsMemoryBucket.class);
    }

    public SharedBetaFactStorage get(AlphaMask mask) {
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

    public boolean init(AlphaMask mask) {
        int bucketIndex = mask.getBucketIndex();
        if (bucketIndex >= alphaBuckets.data.length) {
            FieldsMemoryBucket bucket = new FieldsMemoryBucket(runtime, typeFields, mask);
            this.alphaBuckets.append(bucket);
            return true;
        } else {
            SharedBetaFactStorage storage = alphaBuckets.data[bucketIndex].getFieldData();
            if (storage == null) {
                throw new IllegalArgumentException("No alpha bucket created for " + mask);
            } else {
                return false;
            }
        }
    }

    void clear() {
        for (FieldsMemoryBucket bucket : alphaBuckets.data) {
            bucket.clear();
        }
    }

    @Override
    public void onBeforeChange() {
        RuntimeRules rules = runtime.getRuleStorage();
        RuntimeFactType[][] typesByAlphaBucket = rules.getTypesByAlphaBucket(typeFields);

        for (FieldsMemoryBucket bucket : this.alphaBuckets.data) {
            bucket.setFactTypesByAlpha(typesByAlphaBucket[bucket.getBucketIndex()]);
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

    @Override
    public void onAfterChange() {
        for (FieldsMemoryBucket bucket : alphaBuckets.data) {
            bucket.mergeDelta();
        }
    }
}

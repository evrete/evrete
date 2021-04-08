package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.InnerFactMemory;
import org.evrete.api.SharedBetaFactStorage;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.util.Bits;

import java.util.StringJoiner;

public class FieldsMemory extends MemoryComponent implements InnerFactMemory {
    private final FieldsKey typeFields;
    private final ArrayOf<FieldsMemoryBucket> alphaBuckets;

    FieldsMemory(MemoryComponent runtime, FieldsKey typeFields) {
        super(runtime);
        this.typeFields = typeFields;
        this.alphaBuckets = new ArrayOf<>(FieldsMemoryBucket.class);
    }


    @Override
    protected void clearLocalData() {
        // Only child data present
    }

    @Override
    void insert(LazyValues key, Bits alphaTests, FactHandleVersioned value) {
        alphaBuckets.forEach(bucket -> bucket.insert(key, alphaTests, value));
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
        for (FieldsMemoryBucket bucket : alphaBuckets.data) {
            bucket.commitChanges();
        }
    }

    FieldsMemoryBucket getCreate(AlphaBucketMeta alphaMeta) {
        return alphaBuckets.computeIfAbsent(alphaMeta.getBucketIndex(), k -> new FieldsMemoryBucket(FieldsMemory.this, typeFields, alphaMeta));
    }


    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner("\n");
        alphaBuckets.forEach(bucket -> sj.add(bucket.toString()));
        return sj.toString();
    }
}

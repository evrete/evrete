package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.InnerFactMemory;
import org.evrete.api.KeyedFactStorage;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.util.Bits;

import java.util.StringJoiner;
import java.util.function.Consumer;

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

    public void forEachBucket(Consumer<? super FieldsMemoryBucket> consumer) {
        alphaBuckets.forEach(consumer);
    }

    @Override
    void insert(LazyValues key, Bits alphaTests, FactHandleVersioned value) {
        alphaBuckets.forEach(bucket -> bucket.insert(key, alphaTests, value));
    }

    public KeyedFactStorage get(AlphaBucketMeta mask) {
        int bucketIndex = mask.getBucketIndex();
        if (bucketIndex >= alphaBuckets.data.length) {
            throw new IllegalArgumentException("No alpha bucket created for " + mask);
        } else {
            KeyedFactStorage storage = alphaBuckets.data[bucketIndex].getFieldData();
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

package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.FieldToValueHandle;
import org.evrete.api.SharedBetaFactStorage;
import org.evrete.api.spi.InnerFactMemory;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

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

    @Override
    public void insert(FactHandleVersioned value, FieldToValueHandle key) {
        throw new UnsupportedOperationException();
        //alphaBuckets.forEach(bucket -> bucket.insert(value, key));
    }

    @Override
    void insert(FactHandleVersioned value, LazyInsertState insertState) {
        alphaBuckets.forEach(bucket -> bucket.insert(value, insertState));
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
        alphaBuckets.forEach(new Consumer<FieldsMemoryBucket>() {
            @Override
            public void accept(FieldsMemoryBucket bucket) {
                sj.add(bucket.toString());
            }
        });
        return sj.toString();
    }
}

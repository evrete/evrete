package org.evrete.runtime;

import org.evrete.api.InnerFactMemory;
import org.evrete.api.KeyedFactStorage;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.function.Consumer;

public class KeyMemory extends MemoryComponent implements InnerFactMemory {
    private final FieldsKey typeFields;
    private final ArrayOf<KeyMemoryBucket> alphaBuckets;

    KeyMemory(MemoryComponent runtime, FieldsKey typeFields) {
        super(runtime);
        this.typeFields = typeFields;
        this.alphaBuckets = new ArrayOf<>(KeyMemoryBucket.class);
    }


    @Override
    protected void clearLocalData() {
        // Only child data present
    }

    void forEachBucket(Consumer<? super KeyMemoryBucket> consumer) {
        alphaBuckets.forEach(consumer);
    }

    public ArrayOf<KeyMemoryBucket> getAlphaBuckets() {
        return alphaBuckets;
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
        throw new UnsupportedOperationException();
    }

    void commitBuffer() {
        for (KeyMemoryBucket bucket : alphaBuckets.data) {
            bucket.commitBuffer();
        }
    }

    KeyMemoryBucket getCreate(AlphaBucketMeta alphaMeta) {
        return alphaBuckets.computeIfAbsent(alphaMeta.getBucketIndex(), k -> KeyMemoryBucket.factory(KeyMemory.this, typeFields, alphaMeta));
    }


/*
    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner("\n");
        alphaBuckets.forEach(bucket -> sj.add(bucket.toString()));
        return sj.toString();
    }
*/
}

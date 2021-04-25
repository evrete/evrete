package org.evrete.runtime;

import org.evrete.api.InnerFactMemory;
import org.evrete.api.KeyedFactStorage;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.MemoryAddress;

import java.util.function.Consumer;

public class KeyMemory extends MemoryComponent implements InnerFactMemory {
    private final ArrayOf<KeyMemoryBucket> alphaBuckets;

    KeyMemory(MemoryComponent runtime) {
        super(runtime);
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

    public KeyedFactStorage get(MemoryAddress bucket) {
        return getMemoryBucket(bucket).getFieldData();
    }

    public KeyMemoryBucket getMemoryBucket(MemoryAddress bucket) {
        int bucketIndex = bucket.getBucketIndexOld();
        if (bucketIndex >= alphaBuckets.data.length) {
            throw new IllegalArgumentException("No alpha bucket created for " + bucket);
        } else {
            KeyMemoryBucket storage = alphaBuckets.data[bucketIndex];
            if (storage == null) {
                throw new IllegalArgumentException("No alpha bucket created for " + bucket);
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

    KeyMemoryBucket getCreate(MemoryAddress address) {
        return alphaBuckets.computeIfAbsent(address.getBucketIndexOld(), k -> KeyMemoryBucket.factory(KeyMemory.this, address));
    }
}

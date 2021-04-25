package org.evrete.runtime;

import org.evrete.api.InnerFactMemory;
import org.evrete.api.KeyedFactStorage;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.MemoryAddress;

import java.util.function.Consumer;

public class KeyMemory extends MemoryComponent implements InnerFactMemory {
    private final ArrayOf<KeyMemoryBucket> alphaBuckets;

    KeyMemory(MemoryComponent runtime, MemoryAddress address) {
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
        int bucketIndex = bucket.getBucketIndex();
        if (bucketIndex >= alphaBuckets.data.length) {
            throw new IllegalArgumentException("No alpha bucket created for " + bucket);
        } else {
            KeyedFactStorage storage = alphaBuckets.data[bucketIndex].getFieldData();
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
        return alphaBuckets.computeIfAbsent(address.getBucketIndex(), k -> KeyMemoryBucket.factory(KeyMemory.this, address));
    }
}

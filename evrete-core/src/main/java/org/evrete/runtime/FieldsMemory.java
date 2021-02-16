package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.FieldToValue;
import org.evrete.api.SharedBetaFactStorage;
import org.evrete.api.spi.InnerFactMemory;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.function.Consumer;

public class FieldsMemory extends MemoryComponent implements InnerFactMemory {
    private final FieldsKey typeFields;
    //private final SessionMemory runtime;
    private final ArrayOf<FieldsMemoryBucket> alphaBuckets;

    FieldsMemory(MemoryComponent runtime, FieldsKey typeFields) {
        super(runtime);
        //this.runtime = runtime;
        this.typeFields = typeFields;
        this.alphaBuckets = new ArrayOf<>(FieldsMemoryBucket.class);
    }

    @Override
    protected void forEachChildComponent(Consumer<MemoryComponent> consumer) {
        alphaBuckets.forEach(consumer);
    }

    @Override
    protected void clearLocalData() {
        // Only child data present
    }

    @Override
    public void insert(FactHandleVersioned value, FieldToValue key) {
        alphaBuckets.forEach(bucket -> bucket.insert(value, key));
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

    FieldsMemoryBucket touchMemory(AlphaBucketMeta alphaMeta) {
        int bucketIndex = alphaMeta.getBucketIndex();
        if (alphaBuckets.isEmptyAt(bucketIndex)) {
            FieldsMemoryBucket newBucket = new FieldsMemoryBucket(FieldsMemory.this, typeFields, alphaMeta);
            alphaBuckets.set(bucketIndex, newBucket);
            return newBucket;
        }
        return null;
    }

/*
    <T extends RuntimeFact> void onNewAlphaBucket(AlphaBucketMeta alphaMeta, ReIterator<T> existingFacts) {
        FieldsMemoryBucket newBucket = touchMemory(alphaMeta);
        assert newBucket != null;
        if (existingFacts.reset() > 0) {
            while (existingFacts.hasNext()) {
                RuntimeFact rto = existingFacts.next();
                if (alphaMeta.test(rto)) {
                    //TODO !!!!!!! create tests and resolve the situation
                    throw new UnsupportedOperationException();
                }
            }
        }
    }
*/


/*
    void insert(FactHandle handle, FieldToValue values, boolean[] alphaTests) {
        for (FieldsMemoryBucket bucket : alphaBuckets.data) {
            bucket.insert(handle, values, alphaTests);
        }
    }
*/

/*
    void retract(RuntimeFact fact) {
        for (FieldsMemoryBucket bucket : alphaBuckets.data) {
            bucket.delete(fact);
        }
    }
*/

    @Override
    public String toString() {
        return alphaBuckets.toString();
    }
}

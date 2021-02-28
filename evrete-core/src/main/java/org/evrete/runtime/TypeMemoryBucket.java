package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

class TypeMemoryBucket extends MemoryComponent implements PlainMemory {
    private final SharedPlainFactStorage data;
    private final SharedPlainFactStorage delta;
    private final AlphaBucketMeta alphaMask;

    TypeMemoryBucket(MemoryComponent parent, AlphaBucketMeta alphaMask) {
        super(parent);
        this.data = memoryFactory.newPlainStorage(TypeField.ZERO_ARRAY);
        this.delta = memoryFactory.newPlainStorage(TypeField.ZERO_ARRAY);
        this.alphaMask = alphaMask;
    }

    @Override
    protected void clearLocalData() {
        data.clear();
        delta.clear();
    }

    @Override
    void insert(FactHandleVersioned value, LazyInsertState insertState) {
        FieldToValueHandle key = insertState.record;
        if (insertState.test(alphaMask)) {
            delta.insert(key, value);
        }
    }

    @Override
    public ReIterator<FactHandleVersioned> mainIterator() {
        return data.iterator();
    }

    @Override
    public ReIterator<FactHandleVersioned> deltaIterator() {
        return delta.iterator();
    }

    @Override
    public void commitChanges() {
        if (delta.size() > 0) {
            data.insert(delta);
            delta.clear();
        }
    }

    @Override
    public String toString() {
        return "{" +
                "data=" + data +
                ", delta=" + delta +
                '}';
    }
}

package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.FieldToValueHandle;
import org.evrete.api.ReIterator;
import org.evrete.api.SharedPlainFactStorage;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

class TypeMemoryBucket extends MemoryComponent implements PlainMemory {
    private final SharedPlainFactStorage data;
    private final SharedPlainFactStorage delta;
    private final AlphaBucketMeta alphaMask;

    TypeMemoryBucket(MemoryComponent parent, AlphaBucketMeta alphaMask) {
        super(parent);
        this.data = memoryFactory.newPlainStorage();
        this.delta = memoryFactory.newPlainStorage();
        this.alphaMask = alphaMask;
    }

    @Override
    protected void clearLocalData() {
        data.clear();
        delta.clear();
    }

    @Override
    public void insert(FactHandleVersioned value, FieldToValueHandle key) {
        if (alphaMask.test(valueResolver, key)) {
            delta.insert(value, key);
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
}

package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.FieldToValueHandle;
import org.evrete.api.ReIterator;
import org.evrete.api.SharedPlainFactStorage;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.function.Consumer;

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
    protected void forEachChildComponent(Consumer<MemoryComponent> consumer) {
        // No child components
    }

    @Override
    protected void clearLocalData() {
        data.clear();
        delta.clear();
    }

    @Override
    public void insert(FactHandleVersioned value, FieldToValueHandle key) {
        if (alphaMask.test(memoryFactory.getValueResolver(), key)) {
            delta.insert(value, key);
        }
    }



/*
    <T extends RuntimeFact> void fillMainStorage(ReIterator<T> iterator) {
        //throw new UnsupportedOperationException();
        if (iterator.reset() > 0) {
            while (iterator.hasNext()) {
                RuntimeFact rto = iterator.next();
                if (alphaMask.test(rto)) {
                    data.insert(rto);
                }
            }
        }
    }
*/

    public AlphaBucketMeta getAlphaMask() {
        return alphaMask;
    }

    public SharedPlainFactStorage getData() {
        return data;
    }

    public SharedPlainFactStorage getDelta() {
        return delta;
    }

    @Override
    public boolean hasChanges() {
        return delta.size() > 0;
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

/*
    void insert(ReIterable<? extends RuntimeFact> facts) {
        ReIterator<? extends RuntimeFact> it = facts.iterator();
        delta.ensureExtraCapacity((int) it.reset());
        while (it.hasNext()) {
            insert(it.next());
        }
    }
*/

/*
    void insert(FactHandle handle, boolean[] alphaTests) {
        if (alphaMask.test(alphaTests)) {
            delta.insert(handle);
        }
    }
*/

    @Override
    public String toString() {
        return "{" +
                "data=" + data +
                ", delta=" + delta +
                ", alphaMask=" + alphaMask +
                '}';
    }
}

package org.evrete.runtime.memory;

import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;
import org.evrete.api.SharedPlainFactStorage;
import org.evrete.runtime.PlainMemory;
import org.evrete.runtime.RuntimeObject;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.Collection;

class TypeMemoryBucket implements PlainMemory {
    private final SharedPlainFactStorage data;
    private final SharedPlainFactStorage delta;
    private final AlphaBucketMeta alphaMask;

    TypeMemoryBucket(SessionMemory runtime, AlphaBucketMeta alphaMask) {
        assert !alphaMask.isEmpty();
        this.data = runtime.newSharedPlainStorage();
        this.delta = runtime.newSharedPlainStorage();
        this.alphaMask = alphaMask;
    }

    void clear() {
        this.data.clear();
        this.delta.clear();
    }

    void fillMainStorage(ReIterator<RuntimeObject> iterator) {
        if (iterator.reset() > 0) {
            while (iterator.hasNext()) {
                RuntimeObject rto = iterator.next();
                if (alphaMask.test(rto)) {
                    data.insert(rto);
                }
            }
        }
    }

    @Override
    public boolean hasChanges() {
        return delta.size() > 0;
    }

    @Override
    public ReIterator<RuntimeFact> mainIterator() {
        return data.iterator();
    }

    @Override
    public ReIterator<RuntimeFact> deltaIterator() {
        return delta.iterator();
    }

    @Override
    public void commitChanges() {
        int deltaSize;
        if ((deltaSize = delta.size()) > 0) {
            //TODO !!!!bulk insert, change interface, use the same approach in hot deployment
            data.ensureExtraCapacity(deltaSize);
            delta.iterator().forEachRemaining(data::insert);
            delta.clear();
        }
    }

    void insert(Collection<RuntimeObject> facts) {
        delta.ensureExtraCapacity(facts.size());
        for (RuntimeObject rto : facts) {
            if (alphaMask.test(rto)) {
                delta.insert(rto);
            }
        }
    }

    void retract(Collection<RuntimeFact> facts) {
        for (RuntimeFact fact : facts) {
            if (alphaMask.test(fact)) {
                data.delete(fact);
            }
        }
    }
}

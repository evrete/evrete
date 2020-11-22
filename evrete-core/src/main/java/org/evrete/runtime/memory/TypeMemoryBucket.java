package org.evrete.runtime.memory;

import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;
import org.evrete.api.SharedPlainFactStorage;
import org.evrete.runtime.PlainMemory;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.Collection;

class TypeMemoryBucket implements PlainMemory {
    private final SharedPlainFactStorage data;
    private final SharedPlainFactStorage delta;
    private final AlphaBucketMeta alphaMask;

    TypeMemoryBucket(SessionMemory runtime, AlphaBucketMeta alphaMask) {
        this.data = runtime.newSharedPlainStorage();
        this.delta = runtime.newSharedPlainStorage();
        this.alphaMask = alphaMask;
    }

    void clear() {
        this.data.clear();
        this.delta.clear();
    }

    <T extends RuntimeFact> void fillMainStorage(ReIterator<T> iterator) {
        if (iterator.reset() > 0) {
            while (iterator.hasNext()) {
                RuntimeFact rto = iterator.next();
                if (alphaMask.test(rto)) {
                    data.insert(rto);
                }
            }
        }
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
            data.insert(delta);
/*
            //TODO !!!!bulk insert, change interface, use the same approach as in hot deployment
            data.ensureExtraCapacity(deltaSize);
            delta.iterator().forEachRemaining(data::insert);
*/
            delta.clear();
        }
    }

    public void commitDelta() {
        int deltaSize;
        if ((deltaSize = delta.size()) > 0) {
            //TODO !!!!bulk insert, change interface, use the same approach as in hot deployment
            data.ensureExtraCapacity(deltaSize);
            delta.iterator().forEachRemaining(data::insert);
            delta.clear();
        }
    }

    void insert(Collection<RuntimeFact> facts) {
        delta.ensureExtraCapacity(facts.size());
        for (RuntimeFact rto : facts) {
            insert(rto);
        }
    }

    void insert(ReIterable<? extends RuntimeFact> facts) {
        ReIterator<? extends RuntimeFact> it = facts.iterator();
        delta.ensureExtraCapacity((int) it.reset());
        while (it.hasNext()) {
            insert(it.next());
        }
    }

    void insert(RuntimeFact fact) {
        if (alphaMask.test(fact)) {
            delta.insert(fact);
        }
    }

    void retract(Collection<RuntimeFact> facts) {
        for (RuntimeFact fact : facts) {
            retract(fact);
        }
    }

    void retract(ReIterable<? extends RuntimeFact> facts) {
        ReIterator<? extends RuntimeFact> it = facts.iterator();
        while (it.hasNext()) {
            retract(it.next());
        }
    }

    //TODO !!!! optimize, we don't seem to need this method when using lazy delete
    void retract(RuntimeFact fact) {
        if (alphaMask.test(fact)) {
            data.delete(fact);
        }
        if (alphaMask.test(fact)) {
            delta.delete(fact);
        }
    }

    @Override
    public String toString() {
        return "{" +
                "data=" + data +
                ", delta=" + delta +
                ", alphaMask=" + alphaMask +
                '}';
    }
}

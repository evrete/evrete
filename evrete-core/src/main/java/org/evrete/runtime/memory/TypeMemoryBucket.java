package org.evrete.runtime.memory;

import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;
import org.evrete.api.spi.SharedPlainFactStorage;
import org.evrete.runtime.AlphaMask;
import org.evrete.runtime.RuntimeObject;

import java.util.Collection;

class TypeMemoryBucket implements ReIterable<RuntimeFact> {
    private final SharedPlainFactStorage data;
    private final AlphaMask alphaMask;

    TypeMemoryBucket(SessionMemory runtime, AlphaMask alphaMask) {
        assert !alphaMask.isEmpty();
        this.data = runtime.getConfiguration().getCollectionsService().newPlainStorage();
        this.alphaMask = alphaMask;
    }

    void clear() {
        this.data.clear();
    }

    void insert(Collection<RuntimeObject> facts) {
        data.ensureExtraCapacity(facts.size());
        for (RuntimeObject rto : facts) {
            if (alphaMask.test(rto)) {
                data.insert(rto);
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

    @Override
    public ReIterator<RuntimeFact> iterator() {
        return data.iterator();
    }
}

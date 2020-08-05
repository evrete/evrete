package org.evrete.runtime;

import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;
import org.evrete.runtime.memory.SessionMemory;
import org.evrete.runtime.structure.FactType;

public final class RuntimeFactTypePlain extends RuntimeFactType {
    private final ReIterable<RuntimeFact> iterable;

    RuntimeFactTypePlain(SessionMemory runtime, FactType other) {
        super(runtime, other);
        this.iterable = runtime.get(other.getType()).get(other.getAlphaMask());
    }

    @Override
    public ReIterator<RuntimeFact> iterator() {
        return iterable.iterator();
    }
}

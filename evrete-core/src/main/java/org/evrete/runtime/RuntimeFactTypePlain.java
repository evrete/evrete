package org.evrete.runtime;

import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;
import org.evrete.runtime.memory.SessionMemory;

public final class RuntimeFactTypePlain extends RuntimeFactType {
    private final ReIterable<RuntimeFact> iterable;

    RuntimeFactTypePlain(SessionMemory runtime, FactType other) {
        super(runtime, other);
        this.iterable = runtime.get(other.getType()).get(other.getAlphaMask());
    }

    @Override
    boolean isBetaNode() {
        return false;
    }

    @Override
    public boolean isDeleteDeltaAvailable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInsertDeltaAvailable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ReIterator<RuntimeFact> iterator() {
        return iterable.iterator();
    }
}

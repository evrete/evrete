package org.evrete.runtime;

import org.evrete.runtime.memory.SessionMemory;

public final class RuntimeFactTypePlain extends RuntimeFactType {
    private final PlainMemory plainMemory;

    RuntimeFactTypePlain(SessionMemory runtime, FactType other) {
        super(runtime, other);
        this.plainMemory = runtime.get(other.getType()).get(other.getAlphaMask());
    }

    @Override
    public boolean isInsertDeltaAvailable() {
        return plainMemory.hasChanges();
    }

    @Override
    public PlainMemory getSource() {
        return plainMemory;
    }
}

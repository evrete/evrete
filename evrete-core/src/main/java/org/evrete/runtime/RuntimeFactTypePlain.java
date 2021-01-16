package org.evrete.runtime;

public final class RuntimeFactTypePlain extends RuntimeFactType {
    private final PlainMemory plainMemory;

    RuntimeFactTypePlain(SessionMemory runtime, FactType other) {
        super(runtime, other);
        this.plainMemory = runtime.get(other.getType()).get(other.getAlphaMask());
    }

    @Override
    public PlainMemory getSource() {
        return plainMemory;
    }
}

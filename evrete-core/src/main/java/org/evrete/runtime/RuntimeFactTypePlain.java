package org.evrete.runtime;

public final class RuntimeFactTypePlain extends RuntimeFactType {
    private final PlainMemory plainMemory;

    RuntimeFactTypePlain(AbstractKnowledgeSession<?> runtime, FactType other) {
        super(runtime, other);
        this.plainMemory = runtime.getMemory().get(other.getType()).getCreateAlpha(other.getAlphaMask());
    }

    @Override
    public PlainMemory getSource() {
        return plainMemory;
    }
}

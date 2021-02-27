package org.evrete.runtime;

import org.evrete.api.SharedBetaFactStorage;
import org.evrete.api.spi.InnerFactMemory;

public class RuntimeFactTypeKeyed extends RuntimeFactType {
    private final SharedBetaFactStorage keyStorage;

    RuntimeFactTypeKeyed(AbstractKnowledgeSession<?> runtime, FactType other) {
        super(runtime, other);
        this.keyStorage = runtime.getMemory().getBetaFactStorage(other);
    }

    RuntimeFactTypeKeyed(RuntimeFactTypeKeyed other) {
        super(other.getRuntime(), other);
        this.keyStorage = other.keyStorage;
    }

    @Override
    public InnerFactMemory getSource() {
        return keyStorage;
    }
}

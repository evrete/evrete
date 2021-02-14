package org.evrete.runtime;

import org.evrete.api.spi.InnerFactMemory;

public abstract class RuntimeFactType extends FactType {
    private final AbstractKnowledgeSession runtime;

    RuntimeFactType(AbstractKnowledgeSession runtime, FactType other) {
        super(other);
        this.runtime = runtime;
    }

    public static RuntimeFactType factory(FactType type, AbstractKnowledgeSession runtime) {
        if (type.getFields().size() > 0) {
            return new RuntimeFactTypeKeyed(runtime, type);
        } else {
            return new RuntimeFactTypePlain(runtime, type);
        }
    }

    public abstract InnerFactMemory getSource();

    public AbstractKnowledgeSession getRuntime() {
        return runtime;
    }
}

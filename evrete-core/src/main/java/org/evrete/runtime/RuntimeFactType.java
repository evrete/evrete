package org.evrete.runtime;

import org.evrete.api.Memory;
import org.evrete.runtime.memory.SessionMemory;

public abstract class RuntimeFactType extends FactType implements ActivationSubject  {
    private final SessionMemory runtime;

    abstract boolean isBetaNode();

    public abstract Memory getSource();

    RuntimeFactType(SessionMemory runtime, FactType other) {
        super(other);
        this.runtime = runtime;
    }

    public static RuntimeFactType factory(FactType type, SessionMemory runtime) {
        if (type.getFields().size() > 0) {
            return new RuntimeFactTypeKeyed(runtime, type);
        } else {
            return new RuntimeFactTypePlain(runtime, type);
        }
    }



    public SessionMemory getRuntime() {
        return runtime;
    }

    public abstract boolean isInsertDeltaAvailable();

    public abstract boolean isDeleteDeltaAvailable();
}

package org.evrete.runtime;

import org.evrete.api.ReIterable;
import org.evrete.api.RuntimeFact;
import org.evrete.runtime.memory.SessionMemory;
import org.evrete.runtime.structure.FactType;

public abstract class RuntimeFactType extends FactType implements ReIterable<RuntimeFact> {
    public static final RuntimeFactType[] ZERO_ARRAY = new RuntimeFactType[0];
    private final SessionMemory runtime;
    private RuntimeRule rule;

    abstract boolean isBetaNode();

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

    public RuntimeRule getRule() {
        return rule;
    }

    public void setRule(RuntimeRule rule) {
        this.rule = rule;
    }

    public abstract boolean isInsertDeltaAvailable();

    public abstract boolean isDeleteDeltaAvailable();
}

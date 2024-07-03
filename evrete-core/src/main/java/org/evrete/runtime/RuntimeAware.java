package org.evrete.runtime;

public abstract class RuntimeAware {
    protected final AbstractRuntime<?,?> runtime;

    protected RuntimeAware(AbstractRuntime<?, ?> runtime) {
        this.runtime = runtime;
    }
}

package org.evrete.runtime;

import org.evrete.api.RuntimeContext;

public abstract class RuntimeAware<T extends RuntimeContext<?>> {
    private final T runtime;

    public RuntimeAware(T runtime) {
        this.runtime = runtime;
    }

    public final T getRuntime() {
        return runtime;
    }
}

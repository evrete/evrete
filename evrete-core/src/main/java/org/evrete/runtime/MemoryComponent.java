package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.MemoryFactory;
import org.evrete.api.spi.InnerFactMemory;

import java.util.function.Consumer;

abstract class MemoryComponent implements InnerFactMemory {
    protected final MemoryFactory memoryFactory;
    protected final Configuration configuration;

    MemoryComponent(MemoryFactory memoryFactory, Configuration configuration) {
        this.memoryFactory = memoryFactory;
        this.configuration = configuration;
    }

    MemoryComponent(MemoryComponent other) {
        this.memoryFactory = other.memoryFactory;
        this.configuration = other.configuration;
    }

    protected abstract void forEachChildComponent(Consumer<MemoryComponent> consumer);

    protected abstract void clearLocalData();

    final void clear() {
        clearLocalData();
        forEachChildComponent(c -> clear());
    }
}

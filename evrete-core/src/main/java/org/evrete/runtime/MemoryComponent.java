package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.MemoryFactory;
import org.evrete.api.spi.InnerFactMemory;
import org.evrete.collections.ArrayOf;

abstract class MemoryComponent implements InnerFactMemory {
    protected final MemoryFactory memoryFactory;
    protected final Configuration configuration;
    private final ArrayOf<MemoryComponent> childComponents;

    MemoryComponent(MemoryFactory memoryFactory, Configuration configuration) {
        this.memoryFactory = memoryFactory;
        this.configuration = configuration;
        this.childComponents = new ArrayOf<>(MemoryComponent.class);
    }

    MemoryComponent(MemoryComponent parent) {
        this.memoryFactory = parent.memoryFactory;
        this.configuration = parent.configuration;
        this.childComponents = new ArrayOf<>(MemoryComponent.class);
        parent.addChild(this);
    }

    private void addChild(MemoryComponent childComponent) {
        this.childComponents.append(childComponent);
    }

    protected MemoryComponent[] childComponents() {
        return childComponents.data;
    }

    protected abstract void clearLocalData();

    public final void clear() {
        clearLocalData();
        for (MemoryComponent child : childComponents.data) {
            child.clear();
        }
    }
}

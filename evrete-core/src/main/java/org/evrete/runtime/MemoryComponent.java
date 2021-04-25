package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.MemoryFactory;
import org.evrete.api.ValueResolver;
import org.evrete.collections.ArrayOf;

abstract class MemoryComponent {
    final MemoryFactory memoryFactory;
    final Configuration configuration;
    final ValueResolver valueResolver;
    private final ArrayOf<MemoryComponent> childComponents = new ArrayOf<>(MemoryComponent.class);
    final AbstractWorkingMemory<?> runtime;

    MemoryComponent(AbstractWorkingMemory<?> runtime, MemoryFactory memoryFactory) {
        this.memoryFactory = memoryFactory;
        this.configuration = runtime.getConfiguration();
        this.valueResolver = memoryFactory.getValueResolver();
        this.runtime = runtime;
    }

    MemoryComponent(MemoryComponent parent) {
        this.memoryFactory = parent.memoryFactory;
        this.configuration = parent.configuration;
        this.valueResolver = parent.valueResolver;
        this.runtime = parent.runtime;
        parent.addChild(this);
    }

    private void addChild(MemoryComponent childComponent) {
        this.childComponents.append(childComponent);
    }

    protected abstract void clearLocalData();

    public final void clear() {
        clearLocalData();
        for (MemoryComponent child : childComponents.data) {
            child.clear();
        }
    }
}

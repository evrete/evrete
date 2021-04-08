package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.FactHandleVersioned;
import org.evrete.api.FieldToValueHandle;
import org.evrete.api.MemoryFactory;
import org.evrete.api.ValueResolver;
import org.evrete.collections.ArrayOf;
import org.evrete.util.Bits;

abstract class MemoryComponent {
    final MemoryFactory memoryFactory;
    final Configuration configuration;
    final ValueResolver valueResolver;
    private final ArrayOf<MemoryComponent> childComponents = new ArrayOf<>(MemoryComponent.class);

    MemoryComponent(MemoryFactory memoryFactory, Configuration configuration) {
        this.memoryFactory = memoryFactory;
        this.configuration = configuration;
        this.valueResolver = memoryFactory.getValueResolver();
    }

    MemoryComponent(MemoryComponent parent) {
        this.memoryFactory = parent.memoryFactory;
        this.configuration = parent.configuration;
        this.valueResolver = parent.valueResolver;
        parent.addChild(this);
    }

    abstract void insert(FieldToValueHandle key, Bits alphaTests, FactHandleVersioned value);

    abstract void commitChanges();

    private void addChild(MemoryComponent childComponent) {
        this.childComponents.append(childComponent);
    }

    MemoryComponent[] childComponents() {
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

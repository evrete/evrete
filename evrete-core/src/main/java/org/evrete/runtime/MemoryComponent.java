package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.collections.ArrayOf;

import java.util.Collection;

abstract class MemoryComponent implements TypeResolver {
    final MemoryFactory memoryFactory;
    final Configuration configuration;
    final ValueResolver valueResolver;
    private final AbstractRuleSession<?> runtime;
    private final ArrayOf<MemoryComponent> childComponents = new ArrayOf<>(MemoryComponent.class);

    MemoryComponent(AbstractRuleSession<?> runtime, MemoryFactory memoryFactory) {
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

    public AbstractRuleSession<?> getRuntime() {
        return runtime;
    }

    private void addChild(MemoryComponent childComponent) {
        this.childComponents.append(childComponent);
    }

    protected abstract void clearLocalData();

    public final void clear() {
        clearLocalData();
        for (MemoryComponent child : childComponents) {
            child.clear();
        }
    }

    @Override
    public TypeResolver copyOf() {
        return runtime.copyOf();
    }

    @Override
    public <T> Type<T> getType(String name) {
        return runtime.getType(name);
    }

    @Override
    public <T> Type<T> getType(int typeId) {
        return runtime.getType(typeId);
    }

    @Override
    public Collection<Type<?>> getKnownTypes() {
        return runtime.getKnownTypes();
    }

    @Override
    public void wrapType(TypeWrapper<?> typeWrapper) {
        runtime.wrapType(typeWrapper);
    }

    @Override
    public <T> Type<T> declare(String typeName, Class<T> javaType) {
        return runtime.declare(typeName, javaType);
    }

    @Override
    public <T> Type<T> declare(String typeName, String javaType) {
        return runtime.declare(typeName, javaType);
    }

    @Override
    public <T> Type<T> resolve(Object o) {
        return runtime.resolve(o);
    }
}

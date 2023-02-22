package org.evrete.spi.minimal;

import org.evrete.api.MemoryFactory;
import org.evrete.api.RuntimeContext;
import org.evrete.api.spi.MemoryFactoryProvider;

import java.util.WeakHashMap;

public class DefaultMemoryFactoryProvider extends LeastImportantServiceProvider implements MemoryFactoryProvider {
    private final WeakHashMap<RuntimeContext<?>, DefaultMemoryFactory> instances = new WeakHashMap<>();


    @Override
    public MemoryFactory instance(RuntimeContext<?> context) {
        return instances.computeIfAbsent(context, k -> new DefaultMemoryFactory(context));
    }
}

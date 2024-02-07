package org.evrete.spi.minimal;

import java.util.WeakHashMap;

import org.evrete.api.MemoryFactory;
import org.evrete.api.RuntimeContext;
import org.evrete.api.spi.MemoryFactoryProvider;

import aQute.bnd.annotation.spi.ServiceProvider;

@ServiceProvider(value = MemoryFactoryProvider.class)
public class DefaultMemoryFactoryProvider extends LeastImportantServiceProvider implements MemoryFactoryProvider {
    private final WeakHashMap<RuntimeContext<?>, DefaultMemoryFactory> instances = new WeakHashMap<>();


    @Override
    public MemoryFactory instance(RuntimeContext<?> context) {
        return instances.computeIfAbsent(context, k -> new DefaultMemoryFactory(context));
    }
}

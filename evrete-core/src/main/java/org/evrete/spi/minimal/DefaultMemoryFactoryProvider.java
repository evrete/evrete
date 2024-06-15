package org.evrete.spi.minimal;

import org.evrete.api.FactHandle;
import org.evrete.api.RuntimeContext;
import org.evrete.api.spi.MemoryFactory;
import org.evrete.api.spi.MemoryFactoryProvider;

public class DefaultMemoryFactoryProvider extends LeastImportantServiceProvider implements MemoryFactoryProvider {
    //private final WeakHashMap<RuntimeContext<?>, DefaultMemoryFactory> instances = new WeakHashMap<>();

    @Override
    public <FH extends FactHandle> MemoryFactory<FH> instance(RuntimeContext<?> context, Class<FH> factHandleType) {
        return new DefaultMemoryFactory<>();
    }
}

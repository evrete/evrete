package org.evrete.api.spi;

import org.evrete.api.FactHandle;
import org.evrete.api.OrderedServiceProvider;
import org.evrete.api.RuntimeContext;

/**
 * A Java Service Provider Interface (SPI) designed for creating various types
 * of memory components used within the engine.
 */
public interface MemoryFactoryProvider extends OrderedServiceProvider {
    /**
     * @param context - the context from which a new factory is requested
     * @param factHandleType - the type of the fact handles accepted by the factory
     * @return the instance of {@link MemoryFactory}
     */
    <FH extends FactHandle> MemoryFactory<FH> instance(RuntimeContext<?> context, Class<FH> factHandleType);

}

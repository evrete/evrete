package org.evrete.api.spi;

import org.evrete.api.MemoryFactory;
import org.evrete.api.OrderedServiceProvider;
import org.evrete.api.RuntimeContext;

@SuppressWarnings("unused")
public interface MemoryFactoryProvider extends OrderedServiceProvider {
    /**
     * @param requester - the context from which a new service is requested
     * @return the instance of CollectionsService
     */
    MemoryFactory instance(RuntimeContext<?> requester);

}

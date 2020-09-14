package org.evrete.api.spi;

import org.evrete.api.MemoryCollections;
import org.evrete.api.OrderedServiceProvider;
import org.evrete.api.RuntimeContext;

public interface MemoryCollectionsProvider extends OrderedServiceProvider {
    /**
     *
     * @param requester - the context from which a new service is requested
     * @return the instance of CollectionsService
     */
    MemoryCollections instance(RuntimeContext<?> requester);

}

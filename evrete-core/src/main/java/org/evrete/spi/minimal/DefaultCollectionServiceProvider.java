package org.evrete.spi.minimal;

import org.evrete.api.RuntimeContext;
import org.evrete.api.MemoryCollections;
import org.evrete.api.spi.MemoryCollectionsProvider;

public class DefaultCollectionServiceProvider extends LastServiceProvider implements MemoryCollectionsProvider {

    @Override
    public MemoryCollections instance(RuntimeContext<?> requester) {
        return new DefaultCollectionsService();
    }
}

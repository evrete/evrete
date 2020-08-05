package org.evrete.spi.minimal;

import org.evrete.api.spi.CollectionsService;
import org.evrete.api.spi.CollectionsServiceProvider;

import java.util.Properties;

public class DefaultCollectionServiceProvider implements CollectionsServiceProvider {
    private static final int ORDER = Integer.MAX_VALUE;

    @Override
    public CollectionsService instance(Properties properties) {
        return new DefaultCollectionsService();
    }

    @Override
    public int order() {
        return ORDER;
    }
}

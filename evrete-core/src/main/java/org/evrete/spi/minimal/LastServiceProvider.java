package org.evrete.spi.minimal;

import org.evrete.api.OrderedServiceProvider;

abstract class LastServiceProvider implements OrderedServiceProvider {
    private static final int ORDER = Integer.MAX_VALUE;

    @Override
    public int sortOrder() {
        return ORDER;
    }
}

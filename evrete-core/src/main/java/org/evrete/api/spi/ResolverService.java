package org.evrete.api.spi;

import org.evrete.api.TypeResolver;

public interface ResolverService {
    TypeResolver newInstance();
}

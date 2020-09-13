package org.evrete.spi.minimal;

import org.evrete.api.RuntimeContext;
import org.evrete.api.TypeResolver;
import org.evrete.api.spi.TypeResolverProvider;

public class DefaultTypeResolverProvider extends LastServiceProvider implements TypeResolverProvider {

    @Override
    public TypeResolver instance(RuntimeContext<?> requester) {
        return new TypeResolverImpl(requester);
    }
}

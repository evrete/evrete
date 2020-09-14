package org.evrete.api.spi;

import org.evrete.api.OrderedServiceProvider;
import org.evrete.api.RuntimeContext;
import org.evrete.api.TypeResolver;

public interface TypeResolverProvider extends OrderedServiceProvider {

    TypeResolver instance(RuntimeContext<?> requester);

}

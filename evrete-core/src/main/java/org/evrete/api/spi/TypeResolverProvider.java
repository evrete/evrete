package org.evrete.api.spi;

import org.evrete.api.OrderedServiceProvider;
import org.evrete.api.RuntimeContext;
import org.evrete.api.TypeResolver;

public interface TypeResolverProvider extends OrderedServiceProvider {

    default TypeResolver instance(RuntimeContext<?> context) {
        return instance(context.getClassLoader());
    }

    TypeResolver instance(ClassLoader classLoader);
}

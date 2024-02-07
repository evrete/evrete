package org.evrete.spi.minimal;

import org.evrete.api.TypeResolver;
import org.evrete.api.spi.TypeResolverProvider;

import aQute.bnd.annotation.spi.ServiceProvider;

@ServiceProvider(value = TypeResolverProvider.class)
public class DefaultTypeResolverProvider extends LeastImportantServiceProvider implements TypeResolverProvider {

    @Override
    public TypeResolver instance(ClassLoader classLoader) {
        return new DefaultTypeResolver(classLoader);
    }
}

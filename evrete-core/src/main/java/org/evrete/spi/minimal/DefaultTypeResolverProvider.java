package org.evrete.spi.minimal;

import org.evrete.api.TypeStorage;
import org.evrete.api.spi.TypeResolverProvider;

public class DefaultTypeResolverProvider extends LeastImportantServiceProvider implements TypeResolverProvider {

    @Override
    public TypeStorage newStorage() {
        return new DefaultTypeStorage();
    }
}

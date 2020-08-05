package org.evrete.spi.minimal;

import org.evrete.api.TypeResolver;
import org.evrete.api.spi.ResolverService;

public class DefaultResolverService implements ResolverService {
    private final JcCompiler compiler;

    public DefaultResolverService(JcCompiler compiler) {
        this.compiler = compiler;
    }

    @Override
    public TypeResolver newInstance() {
        return new TypeResolverImpl(compiler);
    }
}

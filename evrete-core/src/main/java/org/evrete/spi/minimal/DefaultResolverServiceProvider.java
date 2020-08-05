package org.evrete.spi.minimal;

import org.evrete.api.spi.ResolverService;
import org.evrete.api.spi.ResolverServiceProvider;

import java.util.Properties;

public class DefaultResolverServiceProvider implements ResolverServiceProvider {
    private static final int ORDER = Integer.MAX_VALUE;


    @Override
    public ResolverService instance(Properties properties, ClassLoader classLoader) {
        return new DefaultResolverService(new JcCompiler(classLoader));
    }

    @Override
    public int order() {
        return ORDER;
    }
}

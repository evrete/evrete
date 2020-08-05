package org.evrete.spi.minimal;

import org.evrete.api.spi.ExpressionResolver;
import org.evrete.api.spi.ExpressionResolverProvider;

import java.util.Properties;

public class DefaultExpressionResolverProvider implements ExpressionResolverProvider {
    private static final int ORDER = Integer.MAX_VALUE;


    @Override
    public ExpressionResolver instance(Properties properties, ClassLoader classLoader) {
        return new DefaultExpressionResolver(classLoader);
    }

    @Override
    public int order() {
        return ORDER;
    }
}

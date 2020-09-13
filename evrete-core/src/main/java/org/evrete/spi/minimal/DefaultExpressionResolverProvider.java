package org.evrete.spi.minimal;

import org.evrete.api.RuntimeContext;
import org.evrete.api.ExpressionResolver;
import org.evrete.api.spi.ExpressionResolverProvider;

public class DefaultExpressionResolverProvider extends LastServiceProvider implements ExpressionResolverProvider {

    @Override
    public ExpressionResolver instance(RuntimeContext<?> requester) {
        return new DefaultExpressionResolver(requester);
    }
}

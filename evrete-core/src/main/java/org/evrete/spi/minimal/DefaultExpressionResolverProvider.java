package org.evrete.spi.minimal;

import org.evrete.api.ExpressionResolver;
import org.evrete.api.RuntimeContext;
import org.evrete.api.spi.ExpressionResolverProvider;

import aQute.bnd.annotation.spi.ServiceProvider;

@ServiceProvider(value = ExpressionResolverProvider.class)
public class DefaultExpressionResolverProvider extends LeastImportantServiceProvider implements ExpressionResolverProvider {

    @Override
    public ExpressionResolver instance(RuntimeContext<?> context) {
        return new DefaultExpressionResolver(context);
    }
}

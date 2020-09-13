package org.evrete.api.spi;

import org.evrete.api.ExpressionResolver;
import org.evrete.api.OrderedServiceProvider;
import org.evrete.api.RuntimeContext;

//TODO !!! api docs
public interface ExpressionResolverProvider extends OrderedServiceProvider {
    ExpressionResolver instance(RuntimeContext<?> requester);
}

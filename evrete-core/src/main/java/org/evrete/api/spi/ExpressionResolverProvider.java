package org.evrete.api.spi;

import org.evrete.api.ExpressionResolver;
import org.evrete.api.OrderedServiceProvider;
import org.evrete.api.RuntimeContext;

/**
 * A Service Provider Interface (SPI) for expression parsers.
 */
public interface ExpressionResolverProvider extends OrderedServiceProvider {
    /**
     * <p>
     * Method returns a new or existing expression resolver. If the
     * implementation has a state, developers might want to use the
     * provided {@link RuntimeContext} as a {@link java.lang.ref.WeakReference} key for that state.
     * </p>
     *
     * @param context runtime context from which the resolver is requested
     * @return new or existing instance of {@link ExpressionResolver}
     */
    ExpressionResolver instance(RuntimeContext<?> context);
}

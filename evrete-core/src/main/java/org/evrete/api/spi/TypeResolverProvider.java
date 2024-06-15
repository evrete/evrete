package org.evrete.api.spi;

import org.evrete.api.OrderedServiceProvider;
import org.evrete.api.RuntimeContext;
import org.evrete.api.TypeResolver;

/**
 * The TypeResolverProvider interface represents a service provider interface (SPI)
 * for providing instances of TypeResolver.
 * This interface extends {@link OrderedServiceProvider}, indicating that implementations
 * can have a priority order.
 */
public interface TypeResolverProvider extends OrderedServiceProvider {

    /**
     * Provides an instance of TypeResolver based on the provided RuntimeContext.
     * This is a convenience default method that essentially retrieves the class
     * loader from the context and delegates the work to the {@link #instance(ClassLoader)} method.
     *
     * @param context The runtime context from which the class loader will be retrieved.
     * @return An instance of TypeResolver suitable for the given context.
     * @deprecated use the {@link #instance(ClassLoader)} method instead
     */
    @Deprecated
    default TypeResolver instance(RuntimeContext<?> context) {
        return instance(context.getClassLoader());
    }

    /**
     * Provides an instance of TypeResolver using the specified ClassLoader.
     *
     * @param classLoader The class loader to be used for loading classes and resources.
     * @return An instance of TypeResolver configured to use the given ClassLoader.
     */
    TypeResolver instance(ClassLoader classLoader);

}

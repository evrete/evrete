package org.evrete.api.spi;

import org.evrete.api.OrderedServiceProvider;
import org.evrete.api.RuntimeContext;
import org.evrete.api.TypeResolver;
import org.evrete.api.TypeStorage;

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
     */
    @Deprecated
    default TypeResolver instance(RuntimeContext<?> context) {
        throw new UnsupportedOperationException("Deprecated");
    }

    /**
     * Provides an instance of TypeResolver using the specified ClassLoader.
     *
     * @param classLoader The class loader to be used for loading classes and resources.
     * @return An instance of TypeResolver configured to use the given ClassLoader.
     * @deprecated use the {@link #instance(RuntimeContext)} method instead
     */
    @Deprecated
    default TypeResolver instance(ClassLoader classLoader) {
        throw new UnsupportedOperationException("Deprecated");
    }

    /**
     * Provides an instance of TypeStorage.
     *
     * @return An instance of TypeResolver suitable for the given context.
     */
    TypeStorage newStorage();
}

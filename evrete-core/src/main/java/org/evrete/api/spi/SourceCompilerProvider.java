package org.evrete.api.spi;

import org.evrete.api.OrderedServiceProvider;

/**
 * Provides instances of {@link SourceCompiler}.
 */
public interface SourceCompilerProvider extends OrderedServiceProvider {

    /**
     * Creates a new instance of {@link SourceCompiler} using the specified {@link ClassLoader}.
     *
     * @param classLoader the class loader to be used by the {@link SourceCompiler}.
     * @return a new instance of {@link SourceCompiler}.
     */
    SourceCompiler instance(ClassLoader classLoader);

}

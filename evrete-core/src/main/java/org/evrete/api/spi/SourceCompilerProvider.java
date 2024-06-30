package org.evrete.api.spi;

import org.evrete.api.OrderedServiceProvider;
import org.evrete.api.RuntimeContext;

public interface SourceCompilerProvider extends OrderedServiceProvider {

    SourceCompiler instance(ClassLoader classLoader);

}

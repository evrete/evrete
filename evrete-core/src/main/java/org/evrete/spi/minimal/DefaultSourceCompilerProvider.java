package org.evrete.spi.minimal;

import org.evrete.api.spi.SourceCompiler;
import org.evrete.api.spi.SourceCompilerProvider;
import org.evrete.spi.minimal.compiler.DefaultSourceCompiler;

public class DefaultSourceCompilerProvider implements SourceCompilerProvider {

    @Override
    public SourceCompiler instance(ClassLoader classLoader) {
        return new DefaultSourceCompiler(classLoader);
    }

    @Override
    public int sortOrder() {
        return Integer.MAX_VALUE;
    }
}

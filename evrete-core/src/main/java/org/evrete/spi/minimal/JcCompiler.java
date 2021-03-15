package org.evrete.spi.minimal;

import org.evrete.api.RuntimeContext;
import org.evrete.util.compiler.BytesClassLoader;
import org.evrete.util.compiler.SourceCompiler;

class JcCompiler extends SourceCompiler {
    private final BytesClassLoader classLoader;

    JcCompiler(RuntimeContext<?> ctx) {
        this.classLoader = new BytesClassLoader(ctx.getClassLoader());
    }

    BytesClassLoader getClassLoader() {
        return classLoader;
    }

    Class<?> compile(String source) {
        return compile(source, classLoader);
    }

}

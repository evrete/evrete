package org.evrete.spi.minimal;

import org.evrete.api.RuntimeContext;
import org.evrete.util.compiler.CompiledClassLoader;
import org.evrete.util.compiler.SingleSourceCompiler;

class JcCompiler extends SingleSourceCompiler {
    private final CompiledClassLoader classLoader;

    JcCompiler(RuntimeContext<?> ctx) {
        this.classLoader = new CompiledClassLoader(ctx.getClassLoader());
    }

    CompiledClassLoader getClassLoader() {
        return classLoader;
    }

    Class<?> compile(String source) {
        return compile(source, classLoader);
    }

}

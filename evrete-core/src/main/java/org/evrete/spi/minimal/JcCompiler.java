package org.evrete.spi.minimal;

import org.evrete.api.RuntimeContext;
import org.evrete.util.compiler.SingleSourceCompiler;

class JcCompiler extends SingleSourceCompiler {
    private final JcClassLoader classLoader;

    JcCompiler(RuntimeContext<?> ctx) {
        this.classLoader = new JcClassLoader(ctx.getClassLoader());
    }

    JcClassLoader getClassLoader() {
        return classLoader;
    }

    Class<?> compile(String className, String source) {
        byte[] bytes = super.compileToBytes(className, source, classLoader.getParent());
        return classLoader.buildClass(className, bytes);
    }

}

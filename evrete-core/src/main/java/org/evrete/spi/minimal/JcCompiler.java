package org.evrete.spi.minimal;

import org.evrete.api.RuntimeContext;
import org.evrete.util.compiler.BytesClassLoader;
import org.evrete.util.compiler.CompilationException;
import org.evrete.util.compiler.SourceCompiler;

import java.security.ProtectionDomain;

class JcCompiler extends SourceCompiler {
    private final BytesClassLoader classLoader;

    JcCompiler(RuntimeContext<?> ctx, ProtectionDomain protectionDomain) {
        this.classLoader = new BytesClassLoader(ctx.getClassLoader(), protectionDomain);
    }

    BytesClassLoader getClassLoader() {
        return classLoader;
    }

    Class<?> compile(String source) throws CompilationException {
        return compile(source, classLoader);
    }

}

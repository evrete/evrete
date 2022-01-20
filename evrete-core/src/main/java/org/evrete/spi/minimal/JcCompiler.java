package org.evrete.spi.minimal;

import org.evrete.util.compiler.CompilationException;
import org.evrete.util.compiler.ServiceClassLoader;
import org.evrete.util.compiler.SourceCompiler;

import java.security.ProtectionDomain;

class JcCompiler extends SourceCompiler {
    private final ProtectionDomain protectionDomain;

    JcCompiler(ProtectionDomain protectionDomain) {
        this.protectionDomain = protectionDomain;
    }

    Class<?> compile(ClassLoader classLoader, String source) throws CompilationException {
        ServiceClassLoader cl;
        if(classLoader instanceof ServiceClassLoader) {
            cl = (ServiceClassLoader) classLoader;
        } else {
            cl = new ServiceClassLoader(classLoader, protectionDomain);
        }
        return compile(source, cl);
    }

}

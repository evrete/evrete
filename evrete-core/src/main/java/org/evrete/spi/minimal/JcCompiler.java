package org.evrete.spi.minimal;

import org.evrete.util.compiler.CompilationException;
import org.evrete.util.compiler.ServiceClassLoader;
import org.evrete.util.compiler.SourceCompiler;

class JcCompiler extends SourceCompiler {

    Class<?> compile(ClassLoader classLoader, String source) throws CompilationException {
        ServiceClassLoader cl;
        if(classLoader instanceof ServiceClassLoader) {
            cl = (ServiceClassLoader) classLoader;
        } else {
            cl = new ServiceClassLoader(classLoader);
        }
        return compile(source, cl);
    }

}

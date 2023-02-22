package org.evrete.api;

import org.evrete.api.annotations.NonNull;
import org.evrete.runtime.compiler.CompilationException;

import java.util.Collection;

/**
 * <p>
 *     A plain source Java compiler for current {@link RuntimeContext}.
 *     Compiled classes are automatically added to the current context's classloader and become available
 *     for subsequent compilation tasks, both explicit and implicit (e.g., compiling literal conditions or actions).
 * </p>
 * @see RuntimeContext#getSourceCompiler()
 */
public interface JavaSourceCompiler {

    /**
     * @param sources Java sources to compile
     * @throws CompilationException if compilation failed
     */
    void compile(Collection<String> sources) throws CompilationException;

    /**
     * @param source plain Java source code
     * @return compiled class
     * @throws CompilationException if compilation failed
     */
    Class<?> compile(@NonNull String source) throws CompilationException;


    /**
     * @param binaryName class binary name
     * @param classBytes class bytes
     */
    void  defineClass(String binaryName, byte[] classBytes);
}

package org.evrete.api;

import org.evrete.api.annotations.NonNull;
import org.evrete.runtime.compiler.CompilationException;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 *     A plain source Java compiler for current {@link RuntimeContext}.
 *     Compiled classes are automatically added to the current context's classloader.
 * </p>
 * @see RuntimeContext#getSourceCompiler()
 */
public interface JavaSourceCompiler {

    /**
     * <p>
     *     This method tries to resolve every source's package and class names and calls the {@link #compile(Collection)} afterwards
     * </p>
     *
     * @param sources Java sources to compile
     * @return map that associates sources and their respective compilation results
     * @throws CompilationException if compilation failed
     */
    @Deprecated
    Map<String, Class<?>> compile(Set<String> sources) throws CompilationException;


    /**
     * @param sources Java sources to compile
     * @return compiled classes.
     * @throws CompilationException if compilation failed
     */
    <S extends ClassSource> Collection<Result<S>> compile(Collection<S> sources) throws CompilationException;

    /**
     * @param source plain Java source code
     * @return compiled class
     * @throws CompilationException if compilation failed
     */
    default Class<?> compile(@NonNull String source) throws CompilationException {
        Map<String, Class<?>> m = compile(Collections.singleton(source));
        Class<?> cl = m.get(source);
        if (cl == null) {
            throw new IllegalStateException();
        } else {
            return cl;
        }
    }

    /**
     * @param binaryName class binary name
     * @param classBytes class bytes
     */
    void defineClass(String binaryName, byte[] classBytes);


    interface ClassSource {
        String binaryName();

        String getSource();
    }

    interface Result<S extends ClassSource> {
        S getSource();

        Class<?> getCompiledClass();
    }
}

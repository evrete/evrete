package org.evrete.api.spi;

import org.evrete.api.RuntimeContext;
import org.evrete.util.CompilationException;

import java.util.Collection;

/**
 * <p>
 *     A plain source Java compiler for current {@link RuntimeContext}.
 *     Compiled classes are automatically added to the current context's classloader.
 * </p>
 * @see RuntimeContext#getSourceCompiler()
 */
public interface JavaSourceCompiler {

    /**
     * @param sources Java sources to compile
     * @param <S> the type of class sources
     * @return compiled classes.
     * @throws CompilationException if compilation failed
     */
    <S extends ClassSource> Collection<Result<S>> compile(Collection<S> sources) throws CompilationException;


    /**
     * The ClassSource interface represents a source code file that can be compiled by a Java compiler.
     * It provides methods to retrieve the binary name of the class and the source code content.
     */
    interface ClassSource {
        String binaryName();

        String getSource();
    }

    /**
     * The Result interface represents the result of compiling a source code file using a Java compiler.
     * It provides methods to retrieve the source code and the compiled class.
     *
     * @param <S> the type of the source code file
     */
    interface Result<S extends ClassSource> {
        S getSource();

        Class<?> getCompiledClass();
    }
}

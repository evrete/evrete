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
     * @deprecated in favor of {@link #compileSources(Collection)}
     */
    @Deprecated
    Map<String, Class<?>> compile(Set<String> sources) throws CompilationException;


    /**
     * Compiles a collection of {@link ClassSource} objects into compiled Java classes.
     *
     * @param <S> the type parameter for ClassSource objects
     * @param sources the collection of ClassSource objects to compile
     * @return a {@link CompileResult} object that represents compilation result
     */
    <S extends ClassSource> CompileResult<S> compileSources(Collection<S> sources);

    /**
     * @param sources Java sources to compile
     * @return compiled classes.
     * @throws CompilationException if compilation failed
     * @deprecated in favor of {@link #compileSources(Collection)}
     *
     */
    @Deprecated
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

    interface CompiledSource<S extends ClassSource> {
        S getSource();

        Class<?> getCompiledClass();
    }
    interface FailedSource<S extends ClassSource> {
        S getSource();

        String getFailure();
    }

    interface Failure<S extends ClassSource> {
        Collection<FailedSource<S>> getFailedSources();

        Collection<String> getOtherErrors();
    }

    interface CompileResult<S extends ClassSource> {
        /**
         * <p>
         * Checks whether the compilation was successful or not. This method must be called prior
         * to retrieving success or error components of this result
         * </p>
         *
         * @return {@code true} if the compilation was successful, {@code false} otherwise.
         */
        boolean isSuccessful();

        /**
         * Returns the collection of compiled sources that were successful
         * during the compilation process.
         *
         * @return A collection of CompiledSource objects representing the
         *         compiled sources that were successful.
         * @throws IllegalStateException if the result represents a failure
         * @see #isSuccessful
         */
        Collection<CompiledSource<S>> getSuccess();

        /**
         * Retrieves the failure information from the compilation result.
         *
         * @return A {@link Failure} object containing the failed sources and other error messages.
         * @throws IllegalStateException if the result represents a success
         * @see #isSuccessful
         */
        Failure<S> getFailure();

    }


}

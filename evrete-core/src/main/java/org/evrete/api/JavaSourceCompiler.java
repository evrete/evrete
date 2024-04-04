package org.evrete.api;

import org.evrete.api.annotations.NonNull;
import org.evrete.util.CompilationException;

import java.util.*;

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
     * Resolves plain String java class source into a {@link ClassSource} instance by deriving the class's binary name
     *
     * @param classSource plain Java class source
     * @return resolved {@link ClassSource} instance
     */
    ClassSource resolve(String classSource);

    /**
     * <p>
     *     This method tries to resolve every source's package and class names and calls the {@link #compile(Collection)} afterwards
     * </p>
     *
     * @param sources Java sources to compile
     * @return map that associates sources and their respective compilation results
     * @throws CompilationException if compilation failed
     */
    default Map<String, Class<?>> compile(Set<String> sources) throws CompilationException {
        Map<String, ClassSource> sourceMap = new HashMap<>(sources.size());
        for (String source : sources) {
            sourceMap.put(source, resolve(source));
        }

        Map<String, Class<?>> resultMap = new HashMap<>(sources.size());

        Collection<Result<ClassSource>> results = compile(sourceMap.values());
        for (Result<ClassSource> r : results) {
            resultMap.put(r.getSource().getSource(), r.getCompiledClass());
        }
        return resultMap;
    }


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
     * Defines a class by loading its binary representation into the current runtime context's classloader.
     * This method is used when dynamically generating classes at runtime.
     *
     * @param binaryName  the fully qualified binary name of the class
     * @param classBytes  the byte array of the class's binary representation
     */
    void defineClass(String binaryName, byte[] classBytes);

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

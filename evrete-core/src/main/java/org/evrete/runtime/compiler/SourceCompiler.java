package org.evrete.runtime.compiler;

import org.evrete.api.JavaSourceCompiler;
import org.evrete.api.annotations.NonNull;

import javax.tools.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;

public class SourceCompiler implements JavaSourceCompiler {
    private final static String COMPILER_PARAM_OPTION = "-parameters";
    private final RuntimeClassloader classLoader;
    private final JavaCompiler compiler;

    public SourceCompiler(RuntimeClassloader classLoader) {
        this.classLoader = classLoader;
        this.compiler = Objects.requireNonNull(ToolProvider.getSystemJavaCompiler(), "No Java compiler provided by this platform");
    }

    @Override
    public void defineClass(String binaryName, byte[] classBytes) {
        this.classLoader.saveClass(binaryName, classBytes);
    }

    @Override
    public synchronized Map<String, Class<?>> compile(@NonNull Set<String> sources) throws CompilationException {
        Map<String, ClassSource> sourceMap = new HashMap<>(sources.size());
        for (String source : sources) {
            sourceMap.put(source, JavaSourceObject.parse(source));
        }

        Map<String, Class<?>> resultMap = new HashMap<>(sources.size());

        Collection<Result<ClassSource>> results = compile(sourceMap.values());
        for (Result<ClassSource> r : results) {
            resultMap.put(r.getSource().getSource(), r.getCompiledClass());
        }

        return resultMap;
    }

    @Override
    public <S extends ClassSource> Collection<Result<S>> compile(Collection<S> sources) throws CompilationException {
        Map<String, S> sourcesByClassName = new HashMap<>(sources.size());
        for (S s : sources) {
            sourcesByClassName.put(s.binaryName(), s);
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager systemFm = compiler.getStandardFileManager(diagnostics, null, null)) {

            Collection<JavaSourceObject> parsedSources = sources.stream().map(JavaSourceObject::new).collect(Collectors.toList());

            try (InMemoryFileManager fm = new InMemoryFileManager(systemFm, classLoader)) {

                // Does compiler support the "-parameters" option?
                List<String> parameters = compiler.isSupportedOption(COMPILER_PARAM_OPTION) < 0 ?
                        Collections.emptyList()
                        :
                        Collections.singletonList(COMPILER_PARAM_OPTION);


                boolean success = compiler.getTask(
                        null,
                        fm,
                        diagnostics,
                        parameters,
                        null,
                        parsedSources
                ).call();

                if (success) {
                    Collection<String> binaryNames = new LinkedList<>();
                    for (DestinationClassObject compiled : fm.getOutput()) {
                        classLoader.saveClass(compiled);
                        binaryNames.add(compiled.getBinaryName());
                    }

                    Collection<Class<?>> compiled = new ArrayList<>(binaryNames.size());
                    for (String binaryName : binaryNames) {
                        try {
                            Class<?> cl = Class.forName(binaryName, false, classLoader);
                            compiled.add(cl);
                        } catch (ClassNotFoundException e) {
                            throw new IllegalStateException("Class has been compiled, but can not be resolved", e);
                        }
                    }

                    return compiled.stream()
                            .map(cl -> {
                                String binaryName = cl.getName();
                                final S source = sourcesByClassName.get(binaryName);
                                if (source == null) {
                                    return null;
                                } else {
                                    return new Result<S>() {
                                        @Override
                                        public S getSource() {
                                            return source;
                                        }

                                        @Override
                                        public Class<?> getCompiledClass() {
                                            return cl;
                                        }
                                    };
                                }
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                } else {
                    List<String> otherErrors = new LinkedList<>();
                    Map<ClassSource, String> errorSources = new IdentityHashMap<>();
                    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                        if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                            JavaFileObject errorSource = diagnostic.getSource();
                            String err = diagnostic.toString();
                            if (errorSource instanceof JavaSourceObject) {
                                JavaSourceObject javaSource = (JavaSourceObject) errorSource;
                                errorSources.put(javaSource.getSource(), err);
                            } else {
                                otherErrors.add(err);
                            }
                        }
                    }
                    throw new CompilationException(otherErrors, errorSources);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public <S extends ClassSource> CompileResult<S> compileSources(Collection<S> sources) {
        Map<String, S> sourcesByClassName = new HashMap<>(sources.size());
        for (S s : sources) {
            sourcesByClassName.put(s.binaryName(), s);
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager systemFm = compiler.getStandardFileManager(diagnostics, null, null)) {

            Collection<JavaSourceObject> parsedSources = sources.stream().map(JavaSourceObject::new).collect(Collectors.toList());

            try (InMemoryFileManager fm = new InMemoryFileManager(systemFm, classLoader)) {

                // Does compiler support the "-parameters" option?
                List<String> parameters = compiler.isSupportedOption(COMPILER_PARAM_OPTION) < 0 ?
                        Collections.emptyList()
                        :
                        Collections.singletonList(COMPILER_PARAM_OPTION);


                boolean success = compiler.getTask(
                        null,
                        fm,
                        diagnostics,
                        parameters,
                        null,
                        parsedSources
                ).call();

                if (success) {
                    Collection<String> binaryNames = new LinkedList<>();
                    for (DestinationClassObject compiled : fm.getOutput()) {
                        classLoader.saveClass(compiled);
                        binaryNames.add(compiled.getBinaryName());
                    }

                    Collection<Class<?>> compiled = new ArrayList<>(binaryNames.size());
                    for (String binaryName : binaryNames) {
                        try {
                            Class<?> cl = Class.forName(binaryName, false, classLoader);
                            compiled.add(cl);
                        } catch (ClassNotFoundException e) {
                            throw new IllegalStateException("Class has been compiled, but can not be resolved", e);
                        }
                    }

                    List<CompiledSource<S>> successData = compiled.stream()
                            .map(cl -> {
                                String binaryName = cl.getName();
                                final S source = sourcesByClassName.get(binaryName);
                                if (source == null) {
                                    return null;
                                } else {
                                    return new CompiledSource<S>() {
                                        @Override
                                        public S getSource() {
                                            return source;
                                        }

                                        @Override
                                        public Class<?> getCompiledClass() {
                                            return cl;
                                        }
                                    };
                                }
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    return new CompileResultImpl<>(successData);

                } else {
                    List<String> otherErrors = new LinkedList<>();
                    Collection<FailedSource<S>> failedSources = new LinkedList<>();
                    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                        if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                            JavaFileObject errorSource = diagnostic.getSource();
                            String err = diagnostic.toString();
                            if (errorSource instanceof JavaSourceObject) {
                                JavaSourceObject javaSource = (JavaSourceObject) errorSource;
                                String binaryName = javaSource.getBinaryName();
                                final S source = sourcesByClassName.get(binaryName);
                                if(source != null) {
                                    failedSources.add(new FailedSource<S>() {
                                        @Override
                                        public S getSource() {
                                            return source;
                                        }

                                        @Override
                                        public String getFailure() {
                                            return err;
                                        }
                                    });
                                } else {
                                    otherErrors.add(err);
                                }
                            } else {
                                otherErrors.add(err);
                            }
                        }
                    }

                    Failure<S> failure = new Failure<S>() {
                        @Override
                        public Collection<FailedSource<S>> getFailedSources() {
                            return failedSources;
                        }

                        @Override
                        public Collection<String> getOtherErrors() {
                            return otherErrors;
                        }
                    };

                    return new CompileResultImpl<>(failure);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public <S extends ClassSource> Collection<Result<S>> compileSourcesInner(Collection<S> sources) throws CompilationException {
        Map<String, S> sourcesByClassName = new HashMap<>(sources.size());
        for (S s : sources) {
            sourcesByClassName.put(s.binaryName(), s);
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager systemFm = compiler.getStandardFileManager(diagnostics, null, null)) {

            Collection<JavaSourceObject> parsedSources = sources.stream().map(JavaSourceObject::new).collect(Collectors.toList());

            try (InMemoryFileManager fm = new InMemoryFileManager(systemFm, classLoader)) {

                // Does compiler support the "-parameters" option?
                List<String> parameters = compiler.isSupportedOption(COMPILER_PARAM_OPTION) < 0 ?
                        Collections.emptyList()
                        :
                        Collections.singletonList(COMPILER_PARAM_OPTION);


                boolean success = compiler.getTask(
                        null,
                        fm,
                        diagnostics,
                        parameters,
                        null,
                        parsedSources
                ).call();

                if (success) {
                    Collection<String> binaryNames = new LinkedList<>();
                    for (DestinationClassObject compiled : fm.getOutput()) {
                        classLoader.saveClass(compiled);
                        binaryNames.add(compiled.getBinaryName());
                    }

                    Collection<Class<?>> compiled = new ArrayList<>(binaryNames.size());
                    for (String binaryName : binaryNames) {
                        try {
                            Class<?> cl = Class.forName(binaryName, false, classLoader);
                            compiled.add(cl);
                        } catch (ClassNotFoundException e) {
                            throw new IllegalStateException("Class has been compiled, but can not be resolved", e);
                        }
                    }

                    return compiled.stream()
                            .map(cl -> {
                                String binaryName = cl.getName();
                                final S source = sourcesByClassName.get(binaryName);
                                if (source == null) {
                                    return null;
                                } else {
                                    return new Result<S>() {
                                        @Override
                                        public S getSource() {
                                            return source;
                                        }

                                        @Override
                                        public Class<?> getCompiledClass() {
                                            return cl;
                                        }
                                    };
                                }
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                } else {
                    List<String> otherErrors = new LinkedList<>();
                    Map<ClassSource, String> errorSources = new IdentityHashMap<>();
                    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                        if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                            JavaFileObject errorSource = diagnostic.getSource();
                            String err = diagnostic.toString();
                            if (errorSource instanceof JavaSourceObject) {
                                JavaSourceObject javaSource = (JavaSourceObject) errorSource;
                                errorSources.put(javaSource.getSource(), err);
                            } else {
                                otherErrors.add(err);
                            }
                        }
                    }
                    throw new CompilationException(otherErrors, errorSources);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class CompileResultImpl<S extends ClassSource> implements CompileResult<S> {
        private final boolean success;
        private final Collection<CompiledSource<S>> compiledSources;
        private final Failure<S> failure;

        private CompileResultImpl(boolean success, Collection<CompiledSource<S>> compiledSources, Failure<S> failure) {
            this.success = success;
            this.compiledSources = compiledSources;
            this.failure = failure;
        }
        CompileResultImpl(Failure<S> failure) {
            this(false, null, failure);
        }

        CompileResultImpl(Collection<CompiledSource<S>> compiledSources) {
            this(true, compiledSources, null);
        }

        @Override
        public boolean isSuccessful() {
            return this.success;
        }

        @Override
        public Collection<CompiledSource<S>> getSuccess() {
            if(success) {
                return compiledSources;
            } else {
                throw new IllegalStateException("This result represents a failure");
            }
        }

        @Override
        public Failure<S> getFailure() {
            if(success) {
                throw new IllegalStateException("This result represents a failure");
            } else {
                return failure;
            }
        }
    }
}

package org.evrete.runtime.compiler;

import org.evrete.util.CompilationException;
import org.evrete.api.JavaSourceCompiler;

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
    public ClassSource resolve(String classSource) {
        return JavaSourceObject.parse(classSource);
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
                    Map<ClassSource, List<String>> errorSources = new IdentityHashMap<>();
                    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                        if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                            JavaFileObject errorSource = diagnostic.getSource();
                            String err = diagnostic.toString();
                            if (errorSource instanceof JavaSourceObject) {
                                JavaSourceObject javaSource = (JavaSourceObject) errorSource;
                                errorSources
                                        .computeIfAbsent(javaSource.getSource(), k -> new ArrayList<>())
                                        .add(err);
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
}

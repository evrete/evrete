package org.evrete.runtime.compiler;

import org.evrete.api.JavaSourceCompiler;
import org.evrete.api.annotations.NonNull;

import javax.tools.*;
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

    static String packageName(String binaryName) {
        int dotPos = binaryName.lastIndexOf('.');
        if (dotPos < 0) {
            throw new UnsupportedOperationException("Empty/default packages are not supported");
        }
        return binaryName.substring(0, dotPos);
    }

    @Override
    public void defineClass(String binaryName, byte[] classBytes) {
        this.classLoader.saveClass(binaryName, classBytes);
    }

    @Override
    public synchronized void compile(@NonNull Collection<String> sources) throws CompilationException {
        try {
            compileUnchecked(sources);
        } catch (CompilationException e) {
            throw e;
        } catch (Throwable t) {
            String allSources = String.join("\n", sources);
            throw new CompilationException(t, allSources);
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public Class<?> compile(@NonNull String source) throws CompilationException {
        try {
            Collection<Class<?>> result = compileUnchecked(Collections.singletonList(source))
                    .stream()
                    .map(s -> {
                        try {
                            return Class.forName(s, true, classLoader);
                        } catch (ClassNotFoundException e) {
                            throw new IllegalStateException("Class has been compiled, but can not be resolved", e);
                        }
                    })
                    .collect(Collectors.toList());

            for (Class<?> c : result) {
                if (c.getEnclosingClass() == null) {
                    return c;
                }
            }

            throw new IllegalStateException("No compiled top-level class has been found");

        } catch (CompilationException e) {
            throw e;
        } catch (Throwable t) {
            throw new CompilationException(t, source);
        }
    }

    private Collection<String> compileUnchecked(@NonNull Collection<String> sources) throws Exception {
        Collection<JavaSource> parsedSources = sources
                .stream()
                .map(s -> JavaSource.parse(classLoader.getInstanceId(), s))
                .collect(Collectors.toCollection(LinkedList::new));

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager systemFm = compiler.getStandardFileManager(diagnostics, null, null)) {

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
                        binaryNames.add(compiled.getBinaryName());
                        classLoader.saveClass(compiled);
                    }
                    return binaryNames;
                } else {
                    StringJoiner errors = new StringJoiner(", ");
                    for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                        if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                            errors.add(diagnostic.toString());
                        }
                    }
                    throw new CompilationException("Compilation error(s): " + errors, parsedSources);
                }
            }
        }
    }
}

package org.evrete.util.compiler;


import javax.tools.*;
import java.util.Collections;
import java.util.StringJoiner;

public class SourceCompiler {
    private final JavaCompiler compiler;

    public SourceCompiler() {
        this.compiler = ToolProvider.getSystemJavaCompiler();
    }

    public final Class<?> compile(String source, ServiceClassLoader classLoader) throws CompilationException {
        synchronized (compiler) {
            FileManager<?> fileManager = FileManager.instance(compiler, classLoader);
            byte[] classBytes;
            try {
                // This will make the compiled class preserve argument names in each method
                classBytes = compileInner(fileManager, source, Collections.singletonList("-parameters"));
            } catch (Exception e) {
                // Java compiler might not be supporting the compilation parameters anymore,
                // let's try w/ null compiler arguments
                classBytes = compileInner(fileManager, source, null);
            }
            return classLoader.buildClass(classBytes);
        }
    }

    private byte[] compileInner(FileManager<?> fileManager, String source, Iterable<String> options) throws CompilationException {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        boolean success = compiler.getTask(
                null,
                fileManager,
                diagnostics,
                options,
                null,
                JavaSource.task(source)
        ).call();

        if (!success) {
            StringJoiner errors = new StringJoiner(", ");
            for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    errors.add(diagnostic.toString());
                }
            }
            throw new CompilationException("Compilation error(s): " + errors, source);
        }
        return fileManager.getBytes();
    }
}

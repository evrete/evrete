package org.evrete.util.compiler;


import javax.tools.*;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"WeakerAccess", "unused"})
public class SingleSourceCompiler {
    private final JavaCompiler compiler;

    public SingleSourceCompiler() {
        this.compiler = ToolProvider.getSystemJavaCompiler();
    }

    public final byte[] compileToBytes(String className, String source, ClassLoader classLoader) {
        synchronized (compiler) {
            FileManager<?> fileManager = FileManager.instance(compiler, classLoader);
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            boolean success;
            try {
                success = compiler.getTask(
                        null,
                        fileManager,
                        diagnostics,
                        null,
                        null,
                        JavaSource.task(className, source)
                ).call();
            } catch (Throwable t) {
                throw new CompilationException(t.getCause());
            }

            if (success) {
                return fileManager.getBytes();
            } else {
                List<String> errors = new ArrayList<>();
                for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                    if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                        errors.add(diagnostic.toString());
                    }
                }
                throw new CompilationException("Unknown compilation error: " + errors);
            }
        }
    }

}

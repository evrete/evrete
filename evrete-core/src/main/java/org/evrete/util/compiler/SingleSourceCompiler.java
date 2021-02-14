package org.evrete.util.compiler;


import javax.tools.*;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings({"WeakerAccess", "unused"})
public class SingleSourceCompiler {
    private static final Logger LOGGER = Logger.getLogger(SingleSourceCompiler.class.getName());
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
                StringJoiner errors = new StringJoiner(", ");
                for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                    if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                        errors.add(diagnostic.toString());
                    }
                }
                LOGGER.log(Level.SEVERE, "\n---- UNCOMPILABLE SOURCE START ----\n" + source + "\n----- UNCOMPILABLE SOURCE END -----");
                throw new CompilationException("Unknown compilation error: " + errors + ". Check with error logs for the source code in question.");
            }
        }
    }

}

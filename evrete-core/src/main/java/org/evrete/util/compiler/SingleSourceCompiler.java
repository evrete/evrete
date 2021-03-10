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

    public final Class<?> compile(String source, ClassLoader classLoader) {
        CompiledClassLoader cl;
        if (classLoader instanceof CompiledClassLoader) {
            cl = (CompiledClassLoader) classLoader;
        } else {
            cl = new CompiledClassLoader(classLoader);
        }
        return compile(source, cl);
    }

    public final Class<?> compile(String source, CompiledClassLoader classLoader) {
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
                        JavaSource.task(source)
                ).call();
            } catch (Throwable t) {
                throw new CompilationException(t);
            }

            if (success) {
                return classLoader.buildClass(fileManager.getBytes());
            } else {
                StringJoiner errors = new StringJoiner(", ");
                for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                    if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                        errors.add(diagnostic.toString());
                    }
                }
                LOGGER.log(Level.SEVERE, "\n" + source + "\n");
                throw new CompilationException("Unknown compilation error: " + errors + ". Check with error logs for the source code in question.");
            }
        }
    }

}

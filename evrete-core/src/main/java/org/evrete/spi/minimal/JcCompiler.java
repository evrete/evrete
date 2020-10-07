package org.evrete.spi.minimal;

import javax.tools.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

class JcCompiler {
    private static final Logger LOGGER = Logger.getLogger(JcCompiler.class.getName());
    private final JcClassLoader classLoader;
    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    public JcCompiler(ClassLoader classLoader) {
        this.classLoader = new JcClassLoader(classLoader);
    }

    public Class<?> compile(String className, String source) {
        long t0 = Calendar.getInstance().getTimeInMillis();
        try {
            byte[] bytes = compileSourceToClassBytes(className, source);
            return classLoader.buildClass(className, bytes);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to compile source:\n" + source, e);
            throw new JcCompilationException(e);
        } finally {
            long t1 = Calendar.getInstance().getTimeInMillis();
            LOGGER.fine("Compile time: " + (t1 - t0) + "ms, source: " + source);
        }
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    private byte[] compileSourceToClassBytes(String className, String source) {
        JcFileManager<?> fileManager = JcFileManager.instance(compiler, classLoader.getParent());
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        boolean success;
        try {
            success = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    null,
                    null,
                    JcJavaSource.task(className, source)
            ).call();
        } catch (Throwable t) {
            throw new JcCompilationException(t.getCause());
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
            throw new JcCompilationException("Unknown compilation error: " + errors);
        }
    }

}

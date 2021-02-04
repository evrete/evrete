package org.evrete.spi.minimal;

import org.evrete.api.RuntimeContext;

import javax.tools.*;
import java.util.ArrayList;
import java.util.List;

class JcCompiler {
    private final JcClassLoader classLoader;
    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    JcCompiler(RuntimeContext<?> ctx) {
        this.classLoader = new JcClassLoader(ctx.getClassLoader());
    }

    JcClassLoader getClassLoader() {
        return classLoader;
    }

    Class<?> compile(String className, String source) {
        try {
            byte[] bytes = compileSourceToClassBytes(className, source);
            return classLoader.buildClass(className, bytes);
        } catch (Exception e) {
            throw new JcCompilationException(e);
        }
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

package org.evrete.spi.minimal;

import org.evrete.api.Type;

import javax.tools.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

class JcCompiler {
    private static final Logger LOGGER = Logger.getLogger(JcCompiler.class.getName());
    private static final String FUNCTION_NAME = Function.class.getName();
    private static final String FIELD_NAME = "FUNC";
    private static final String FIELD_TEMPLATE = "" +
            "package %s;\n" +
            "public interface %s {\n" +
            "  %s<%s, Object> %s =\n" +
            "    %s\n" +
            "}\n";

    private static final AtomicInteger counter = new AtomicInteger();
    private final String packageName;
    private final JcClassLoader classLoader;
    private final Map<Type, Map<String, Function<?, ?>>> compiledLambdas = new HashMap<>();
    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private int globalId = 0;


    public JcCompiler(ClassLoader classLoader) {
        this.classLoader = new JcClassLoader(classLoader);
        this.packageName = JcCompiler.class.getPackage().getName() + ".p" + counter.incrementAndGet();
    }

    public final Function<?, ?> compileLambda(TypeImpl type, String exp) {
        String lambda = exp.trim();
        lambda = lambda.endsWith(";") ? lambda : lambda + ';';
        Function<?, ?> function = compiledLambdas.computeIfAbsent(type, k -> new HashMap<>()).get(lambda);

        if (function == null) {
            String className = "F" + (globalId++);
            String source = String.format(FIELD_TEMPLATE,
                    packageName,
                    className,
                    FUNCTION_NAME,
                    type.getClazz().getName(),
                    FIELD_NAME,
                    lambda
            );
            Class<?> compiled = compile(className, source);
            try {
                function = (Function<?, ?>) compiled
                        .getField(FIELD_NAME)
                        .get(null); // Getting value of a static field
                compiledLambdas.get(type).put(lambda, function);
            } catch (JcCompilationException e) {
                throw e;
            } catch (Throwable e) {
                throw new JcCompilationException(e);
            }
        }
        return function;
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

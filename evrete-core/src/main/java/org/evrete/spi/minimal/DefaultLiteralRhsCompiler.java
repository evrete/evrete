package org.evrete.spi.minimal;

import org.evrete.api.Imports;
import org.evrete.api.NamedType;
import org.evrete.api.RhsContext;
import org.evrete.api.RuntimeContext;
import org.evrete.api.spi.LiteralRhsCompiler;
import org.evrete.runtime.compiler.CompilationException;

import aQute.bnd.annotation.spi.ServiceProvider;

import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@ServiceProvider(value = LiteralRhsCompiler.class)
public class DefaultLiteralRhsCompiler extends LeastImportantServiceProvider implements LiteralRhsCompiler {
    private static final AtomicInteger classCounter = new AtomicInteger(0);
    private static final String classPackage = DefaultLiteralRhsCompiler.class.getPackage().getName() + ".rhs";


    @SuppressWarnings("unchecked")
    private static Class<? extends AbstractLiteralRhs> buildClass(RuntimeContext<?> context, NamedType[] types, String literalRhs, Imports imports) throws CompilationException {
        String simpleName = "Rhs" + classCounter.getAndIncrement();
        String source = buildSource(simpleName, types, literalRhs, imports);
        return (Class<? extends AbstractLiteralRhs>) context.getSourceCompiler().compile(source);
    }

    private static String buildSource(String className, NamedType[] types, String literalRhs, Imports imports) {
        StringJoiner methodArgs = new StringJoiner(", ");
        StringJoiner args = new StringJoiner(", ");
        for (NamedType t : types) {
            methodArgs.add(t.getType().getJavaType() + " " + t.getName());
            args.add(t.getName());
        }

        StringBuilder sb = new StringBuilder(2048);
        sb.append("package ").append(classPackage).append(";\n\n");

        imports.asJavaImportStatements(sb);

        sb.append("public class ").append(className).append(" extends ").append(AbstractLiteralRhs.class.getName()).append(" {\n\n");

        // Abstract method
        sb.append("\t@").append(Override.class.getName()).append("\n");
        sb.append("\tprotected void doRhs() {\n");
        for (NamedType t : types) {
            sb.append("\t\t").append(t.getType().getJavaType()).append(" ").append(t.getName()).append(" = ").append("get(\"").append(t.getName()).append("\");\n");

        }
        sb.append("\t\tdoRhs(").append(args).append(");\n");
        sb.append("\t}\n\n");

        // main method
        sb.append("\tprivate void doRhs(").append(methodArgs).append(") {\n");
        sb.append("\t\t/***** Start RHS source *****/\n");
        sb.append(literalRhs).append("\n");
        sb.append("\t\t/****** End RHS source ******/\n");

        sb.append("\t}\n");


        // End of class
        sb.append("}\n");
        return sb.toString();
    }

    @Override
    public Consumer<RhsContext> compileRhs(RuntimeContext<?> context, String literalRhs, NamedType[] types) throws CompilationException {
        try {
            Imports imports = context.getImports();
            Class<? extends AbstractLiteralRhs> clazz = buildClass(context, types, literalRhs, imports);
            return clazz.getDeclaredConstructor().newInstance();
        } catch (CompilationException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException("Failed to compile RHS:\n" + literalRhs);
        }
    }
}

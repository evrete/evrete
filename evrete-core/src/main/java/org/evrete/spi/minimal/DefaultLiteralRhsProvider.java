package org.evrete.spi.minimal;

import org.evrete.api.RhsContext;
import org.evrete.api.RuntimeContext;
import org.evrete.api.spi.LiteralRhsCompiler;
import org.evrete.runtime.FactType;

import java.util.Collection;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class DefaultLiteralRhsProvider extends LeastImportantServiceProvider implements LiteralRhsCompiler {
    private static final AtomicInteger classCounter = new AtomicInteger(0);
    private static final String classPackage = DefaultLiteralRhsProvider.class.getPackage().getName() + ".rhs";

    @SuppressWarnings("unchecked")
    private static Class<? extends AbstractLiteralRhs> buildClass(JcCompiler compiler, FactType[] types, String literalRhs, Collection<String> imports) {
        String className = "Rhs" + classCounter.getAndIncrement();
        String source = buildSource(className, types, literalRhs, imports);
        return (Class<? extends AbstractLiteralRhs>) compiler.compile(className, source);
    }

    @Override
    public Consumer<RhsContext> compileRhs(RuntimeContext<?> requester, String literalRhs, Collection<FactType> factTypes, Collection<String> imports) {
        FactType[] types = factTypes.toArray(FactType.ZERO_ARRAY);

        Class<? extends AbstractLiteralRhs> clazz = buildClass(getCreateJavaCompiler(requester), types, literalRhs, imports);
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize RHS", e);
        }
    }

    private static String buildSource(String className, FactType[] types, String literalRhs, Collection<String> imports) {
        StringJoiner methodArgs = new StringJoiner(", ");
        StringJoiner args = new StringJoiner(", ");
        for (FactType t : types) {
            methodArgs.add(t.getType().getJavaType().getName() + " " + t.getVar());
            args.add(t.getVar());
        }

        StringBuilder sb = new StringBuilder(2048);
        sb.append("package ").append(classPackage).append(";\n\n");

        // Adding imports
        if (!imports.isEmpty()) {
            for (String imp : imports) {
                String s = imp
                        .replaceAll("\\s", "")
                        .replaceAll(";", "");
                sb.append("import ").append(s).append(";\n");
            }
            sb.append("\n");
        }

        sb.append("public class ").append(className).append(" extends ").append(AbstractLiteralRhs.class.getName()).append(" {\n\n");

        // Abstract method
        sb.append("\t@").append(Override.class.getName()).append("\n");
        sb.append("\tprotected void doRhs() {\n");
        for (FactType t : types) {
            sb.append("\t\t").append(t.getType().getJavaType().getName()).append(" ").append(t.getVar()).append(" = ").append("get(\"").append(t.getVar()).append("\");\n");

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
}

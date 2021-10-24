package org.evrete.spi.minimal;

import org.evrete.api.NamedType;
import org.evrete.api.RhsContext;
import org.evrete.api.RuleScope;
import org.evrete.api.RuntimeContext;
import org.evrete.api.spi.LiteralRhsCompiler;
import org.evrete.util.compiler.CompilationException;

import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class DefaultLiteralRhsCompiler extends LeastImportantServiceProvider implements LiteralRhsCompiler {
    private static final AtomicInteger classCounter = new AtomicInteger(0);
    private static final String classPackage = DefaultLiteralRhsCompiler.class.getPackage().getName() + ".rhs";

    @SuppressWarnings("unchecked")
    private static Class<? extends AbstractLiteralRhs> buildClass(ClassLoader classLoader, JcCompiler compiler, NamedType[] types, String literalRhs, Collection<String> imports) throws CompilationException {
        String simpleName = "Rhs" + classCounter.getAndIncrement();
        String source = buildSource(simpleName, types, literalRhs, imports);
        return (Class<? extends AbstractLiteralRhs>) compiler.compile(classLoader, source);
    }

    private static String buildSource(String className, NamedType[] types, String literalRhs, Collection<String> imports) {
        StringJoiner methodArgs = new StringJoiner(", ");
        StringJoiner args = new StringJoiner(", ");
        for (NamedType t : types) {
            methodArgs.add(t.getType().getJavaType().getName() + " " + t.getName());
            args.add(t.getName());
        }

        StringBuilder sb = new StringBuilder(2048);
        sb.append("package ").append(classPackage).append(";\n\n");

        // Adding imports
        if (!imports.isEmpty()) {
            for (String imp : imports) {
                sb.append("import ").append(imp).append(";\n");
            }
            sb.append("\n");
        }

        sb.append("public class ").append(className).append(" extends ").append(AbstractLiteralRhs.class.getName()).append(" {\n\n");

        // Abstract method
        sb.append("\t@").append(Override.class.getName()).append("\n");
        sb.append("\tprotected void doRhs() {\n");
        for (NamedType t : types) {
            sb.append("\t\t").append(t.getType().getJavaType().getName()).append(" ").append(t.getName()).append(" = ").append("get(\"").append(t.getName()).append("\");\n");

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
    public Consumer<RhsContext> compileRhs(RuntimeContext<?> requester, String literalRhs, Collection<NamedType> factTypes, Collection<String> imports) throws CompilationException {

        NamedType[] types = factTypes.toArray(new NamedType[0]);

        ProtectionDomain domain = requester.getService().getSecurity().getProtectionDomain(RuleScope.RHS);
        Class<? extends AbstractLiteralRhs> clazz = buildClass(requester.getClassLoader(), getCreateJavaCompiler(requester, domain), types, literalRhs, imports);
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize RHS", e);
        }
    }
}

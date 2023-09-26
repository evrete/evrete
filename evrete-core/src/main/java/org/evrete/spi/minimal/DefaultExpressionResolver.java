package org.evrete.spi.minimal;

import org.evrete.api.*;
import org.evrete.api.annotations.NonNull;
import org.evrete.runtime.compiler.CompilationException;

import java.lang.invoke.MethodHandle;
import java.util.*;
import java.util.stream.Collectors;

import static org.evrete.spi.minimal.ConditionStringTerm.resolveTerms;

class DefaultExpressionResolver implements ExpressionResolver {
    static final String SPI_LHS_STRIP_WHITESPACES = "evrete.spi.compiler.lhs-strip-whitespaces";

    private final RuntimeContext<?> context;
    private final boolean stripWhitespaces;

    DefaultExpressionResolver(RuntimeContext<?> context) {
        this.context = context;
        this.stripWhitespaces = context.getConfiguration().getAsBoolean(SPI_LHS_STRIP_WHITESPACES, true);
    }

    @Override
    @NonNull
    public FieldReference resolve(String arg, NamedType.Resolver resolver) {
        Type<?> type;
        TypeField field;
        NamedType typeRef;

        int firstDot = arg.indexOf('.');
        if (firstDot < 0) {
            // Var references type
            typeRef = resolver.resolve(arg);
            field = typeRef
                    .getType()
                    .getField(""); // empty value has a special meaning of "this" field
        } else {
            // Var references field
            String lhsFactType = arg.substring(0, firstDot);
            String dottedProp = arg.substring(firstDot + 1);
            Const.assertName(dottedProp);
            Const.assertName(lhsFactType.substring(1));

            typeRef = resolver.resolve(lhsFactType);
            type = typeRef.getType();
            field = type.getField(dottedProp);
        }
        return new FieldReferenceImpl(typeRef, field);
    }

    @Override
    public Collection<LiteralEvaluator> buildExpressions(Collection<LiteralExpression> expressions) throws CompilationException {
        Collection<EvaluatorClassSource> sources = expressions.stream()
                .parallel()
                .map(expression -> {
                    NamedType.Resolver resolver = expression.getContext();
                    StringLiteralEncoder encoder = StringLiteralEncoder.of(expression.getSource(), stripWhitespaces);

                    final List<ConditionStringTerm> terms = resolveTerms(encoder.getEncoded(), s -> resolve(s, resolver));
                    return new EvaluatorClassSource(context, expression, encoder, terms);
                })
                .collect(Collectors.toList());

        Map<String, EvaluatorClassSource> classNameMap = new HashMap<>(sources.size());
        List<String> plainJavaSources = new ArrayList<>(sources.size());
        for(EvaluatorClassSource s : sources) {
            classNameMap.put(s.getClassName(), s);
            plainJavaSources.add(s.getFullJavaSource());
        }

        // Compile all sources
        JavaSourceCompiler compiler = context.getSourceCompiler();

        ClassLoader classLoader = compiler.getClassLoader();
        compiler.compile(plainJavaSources);

        // Retrieve compiled classes
        List<LiteralEvaluator> result = new ArrayList<>(sources.size());

        for(Map.Entry<String, EvaluatorClassSource> entry : classNameMap.entrySet()) {
            String className = entry.getKey();
            EvaluatorClassSource source = entry.getValue();

            try {
                Class<?> compiledClass = Class.forName(className, true, classLoader);
                MethodHandle handle = getHandle(compiledClass);
                result.add(new CompiledEvaluator(handle, source));
            } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
                throw new CompilationException(e, source.getExpression().getSource());
            }
        }

        return result;
    }

    static MethodHandle getHandle(Class<?> compiledClass) throws NoSuchFieldException, IllegalAccessException {
        return (MethodHandle) compiledClass.getDeclaredField("HANDLE").get(null);
    }

}

package org.evrete.spi.minimal;

import org.evrete.api.*;
import org.evrete.api.annotations.NonNull;
import org.evrete.runtime.compiler.CompilationException;
import org.evrete.runtime.compiler.LHSCompilationException;

import java.lang.invoke.MethodHandle;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
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
    public Collection<LiteralEvaluator> buildExpressions(Collection<LiteralExpression> expressions) throws LHSCompilationException {
        Collection<EvaluatorClassSource> sources = expressions
                .stream()
                .parallel()
                .map(expression -> {
                    NamedType.Resolver resolver = expression.getContext();
                    StringLiteralEncoder encoder = StringLiteralEncoder.of(expression.getSource(), stripWhitespaces);

                    final List<ConditionStringTerm> terms = ConditionStringTerm.resolveTerms(encoder.getEncoded(), s -> resolve(s, resolver));
                    return new EvaluatorClassSource(context, expression, encoder, terms);
                })
                .collect(Collectors.toList());

        // Compile all sources
        JavaSourceCompiler.CompileResult<EvaluatorClassSource> compileResult = context.getSourceCompiler()
                .compileSources(sources);

        if(compileResult.isSuccessful()) {
            List<LiteralEvaluator> result = new ArrayList<>(sources.size());
            Collection<JavaSourceCompiler.CompiledSource<EvaluatorClassSource>> successData = compileResult.getSuccess();
            for(JavaSourceCompiler.CompiledSource<EvaluatorClassSource> entry : successData) {
                EvaluatorClassSource source = entry.getSource();
                Class<?> compiledClass = entry.getCompiledClass();
                try {
                    MethodHandle handle = getHandle(compiledClass);
                    result.add(new CompiledEvaluator(handle, source));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
            return result;
        } else {
            JavaSourceCompiler.Failure<EvaluatorClassSource> failureData = compileResult.getFailure();
            List<LHSCompilationException.Failure> details = getFailures(failureData);

            throw new LHSCompilationException(details, failureData.getOtherErrors());
        }
    }

    private static List<LHSCompilationException.Failure> getFailures(JavaSourceCompiler.Failure<EvaluatorClassSource> failureData) {
        List<LHSCompilationException.Failure> details = new ArrayList<>(failureData.getFailedSources().size());
        for(JavaSourceCompiler.FailedSource<EvaluatorClassSource> failed : failureData.getFailedSources()) {
            String error = failed.getFailure();
            LiteralExpression expression = failed.getSource().getExpression();
            LHSCompilationException.Failure failure = new LHSCompilationException.Failure(failed.getSource(), expression, error);
            details.add(failure);
        }
        return details;
    }

    static MethodHandle getHandle(Class<?> compiledClass) throws NoSuchFieldException, IllegalAccessException {
        return (MethodHandle) compiledClass.getDeclaredField("HANDLE").get(null);
    }

}

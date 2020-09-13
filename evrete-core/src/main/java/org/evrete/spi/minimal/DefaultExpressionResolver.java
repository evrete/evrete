package org.evrete.spi.minimal;

import org.evrete.api.*;
import org.evrete.runtime.builder.FieldReference;
import org.evrete.util.NextIntSupplier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class DefaultExpressionResolver implements ExpressionResolver {
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("\\$[a-zA-Z0-9]+(\\.[a-zA-Z][a-zA-Z0-9]*)+");

    private final EvaluatorCompiler evaluatorCompiler;

    DefaultExpressionResolver(RuntimeContext<?> requester) {
        this.evaluatorCompiler = new EvaluatorCompiler(requester.getClassLoader());
    }

    @Override
    public FieldReference resolve(String s, Function<String, NamedType> resolver) {
        int firstDot = s.indexOf('.');
        if (firstDot < 0) {
            throw new IllegalArgumentException("No field detected in '" + s + "'. The minimal implementation expects type and field, e.g. '$a.customer.id'");
        }

        String lhsFactTYpe = s.substring(0, firstDot);
        String dottedProp = s.substring(firstDot + 1);
        Const.assertName(dottedProp);
        Const.assertName(lhsFactTYpe.substring(1));

        NamedType typeRef;
        if ((typeRef = resolver.apply(lhsFactTYpe)) == null) {
            throw new IllegalArgumentException("There's no declared reference '" + lhsFactTYpe + "' in provided context.");
        }


        Type<?> type = typeRef.getType();
        TypeField field = type.getField(dottedProp);
        if (field == null) {
            if (type instanceof TypeImpl) {
                field = ((TypeImpl<?>) type).inspectClass(dottedProp);
            }
            if (field == null) {
                throw new IllegalArgumentException("Unable to resolve property '" + dottedProp + "' of " + type);
            }
        }

        return new FieldReferenceImpl(typeRef, field);
    }

    @Override
    public synchronized Evaluator buildExpression(String rawExpression, Function<String, NamedType> resolver) {
        StringLiteralRemover remover = StringLiteralRemover.of(rawExpression);
        String strippedExpression = remover.getConverted();

        Matcher m = REFERENCE_PATTERN.matcher(strippedExpression);
        List<ConditionStringTerm> terms = new ArrayList<>();
        //Map<Term, JavaSourceFieldReference> resolvedExpressions = new HashMap<>();
        NextIntSupplier fieldCounter = new NextIntSupplier();
        while (m.find()) {
            int matcherEnd = m.end();

            int start, end;

            start = m.start();
            if (matcherEnd == strippedExpression.length()) {
                end = matcherEnd;
            } else {
                char next = strippedExpression.charAt(matcherEnd);
                if (next == '(') {
                    // Match is a method
                    end = strippedExpression.lastIndexOf('.');
                    if (end < 0) throw new IllegalStateException("Something went wrong");
                } else {
                    end = matcherEnd;
                }
            }

            String s = strippedExpression.substring(start, end);
            FieldReference fieldReference = resolve(s, resolver);


            ConditionStringTerm t = new ConditionStringTerm(start, end, fieldReference, fieldCounter);
            terms.add(t);
        }

        return evaluatorCompiler.buildExpression(remover, strippedExpression, terms);
    }

}

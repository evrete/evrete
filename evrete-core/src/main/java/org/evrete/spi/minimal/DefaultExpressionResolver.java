package org.evrete.spi.minimal;

import org.evrete.api.*;
import org.evrete.runtime.builder.FieldReference;
import org.evrete.util.BaseConditionClass;
import org.evrete.util.NextIntSupplier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class DefaultExpressionResolver implements ExpressionResolver {
    private static final String BASE_CLASS_PROPERTY = "org.evrete.minimal.condition-base-class";

    private static final Pattern REFERENCE_PATTERN = Pattern.compile("\\$[a-zA-Z0-9]+(\\.[a-zA-Z][a-zA-Z0-9]*)+");

    private final EvaluatorCompiler evaluatorCompiler;
    private final String conditionBaseClassName;

    DefaultExpressionResolver(RuntimeContext<?> requester) {
        this.evaluatorCompiler = new EvaluatorCompiler(requester.getClassLoader());
        this.conditionBaseClassName = requester.getConfiguration().getProperty(BASE_CLASS_PROPERTY, BaseConditionClass.class.getName());
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
            throw new IllegalArgumentException("Unable to resolve property '" + dottedProp + "' of the type " + type);
        }

        return new FieldReferenceImpl(typeRef, field);
    }

    @Override
    public synchronized Evaluator buildExpression(String rawExpression, Function<String, NamedType> resolver) {
        StringLiteralRemover remover = StringLiteralRemover.of(rawExpression);
        String strippedExpression = remover.getConverted();
        Matcher m = REFERENCE_PATTERN.matcher(strippedExpression);
        List<ConditionStringTerm> terms = new ArrayList<>();

        NextIntSupplier fieldCounter = new NextIntSupplier();
        while (m.find()) {
            int start = m.start(), end = m.end(), actualEnd = end;
            if (end < strippedExpression.length() && strippedExpression.charAt(end) == '(') {
                // The last group is a method call that needs to be effectively stripped off
                // by moving the actualEnd to the rightmost dot
                actualEnd = strippedExpression.substring(start, end).lastIndexOf('.') + start;
            }

            String s = strippedExpression.substring(start, actualEnd);
            FieldReference fieldReference = resolve(s, resolver);


            ConditionStringTerm t = new ConditionStringTerm(start, actualEnd, fieldReference, fieldCounter);
            terms.add(t);
        }

        try {
            Class<?> baseClass = evaluatorCompiler.getClassLoader().loadClass(conditionBaseClassName);
            return evaluatorCompiler.buildExpression(baseClass, remover, strippedExpression, terms);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load class '" + conditionBaseClassName + "'");
        }

    }

}

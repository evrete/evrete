package org.evrete.spi.minimal;

import org.evrete.api.*;
import org.evrete.api.annotations.NonNull;
import org.evrete.runtime.compiler.CompilationException;
import org.evrete.util.NextIntSupplier;
import org.evrete.util.StringLiteralRemover;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.evrete.Configuration.CONDITION_BASE_CLASS;

class DefaultExpressionResolver implements ExpressionResolver {
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("\\$[a-zA-Z0-9]+(\\.[_a-zA-Z][_a-zA-Z0-9]*)*");

    private final EvaluatorCompiler evaluatorCompiler;
    private final RuntimeContext<?> context;

    DefaultExpressionResolver(RuntimeContext<?> context) {
        this.evaluatorCompiler = new EvaluatorCompiler(context);
        this.context = context;
    }

    private static ConditionStringTerm resolveTerm(int start, int actualEnd, FieldReference ref, NextIntSupplier fieldCounter, List<ConditionStringTerm> terms) {
        // Scanning existing terms
        for (ConditionStringTerm t : terms) {
            if (t.type().equals(ref.type()) && t.field().equals(ref.field())) {
                // Found the same reference
                return new ConditionStringTerm(start, actualEnd, t);
            }
        }
        return new ConditionStringTerm(start, actualEnd, ref, fieldCounter);
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
    @NonNull
    public synchronized Evaluator buildExpression(String expression, NamedType.Resolver resolver) throws CompilationException {
        try {
            // Getting the base class for condition
            String conditionBaseClass = context
                    .getConfiguration()
                    .getProperty(CONDITION_BASE_CLASS, BaseConditionClass.class.getName());
            Imports imports = context.getImports();
            try {
                return buildExpression(expression, conditionBaseClass, resolver, imports, true);
            } catch (Throwable e) {
                // Trying again with the original expression. There might be a keyword like 'new',
                // which requires whitespaces to be preserved.
                return buildExpression(expression, conditionBaseClass, resolver, imports, false);
            }
        } catch (CompilationException e) {
            throw e;
        } catch (Throwable t) {
            throw new CompilationException(t, expression);
        }
    }

    private Evaluator buildExpression(String rawExpression, String conditionBaseClassName, NamedType.Resolver resolver, Imports imports, boolean stripWhiteSpaces) throws CompilationException {
        StringLiteralRemover remover = StringLiteralRemover.of(rawExpression, stripWhiteSpaces);
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


            ConditionStringTerm t = resolveTerm(start, actualEnd, fieldReference, fieldCounter, terms);
            terms.add(t);
        }

        return evaluatorCompiler.buildExpression(conditionBaseClassName, remover, strippedExpression, terms, imports);
    }

}

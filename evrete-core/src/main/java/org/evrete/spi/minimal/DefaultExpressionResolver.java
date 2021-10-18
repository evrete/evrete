package org.evrete.spi.minimal;

import org.evrete.api.*;
import org.evrete.util.BaseConditionClass;
import org.evrete.util.NextIntSupplier;
import org.evrete.util.StringLiteralRemover;
import org.evrete.util.compiler.CompilationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class DefaultExpressionResolver implements ExpressionResolver {
    private static final String BASE_CLASS_PROPERTY = "evrete.impl.condition-base-class";

    private static final Pattern REFERENCE_PATTERN = Pattern.compile("\\$[a-zA-Z0-9]+(\\.[_a-zA-Z][_a-zA-Z0-9]*)*");

    private final EvaluatorCompiler evaluatorCompiler;
    private final String conditionBaseClassName;

    DefaultExpressionResolver(RuntimeContext<?> requester, JcCompiler compiler) {
        this.evaluatorCompiler = new EvaluatorCompiler(compiler);
        this.conditionBaseClassName = requester.getConfiguration().getProperty(BASE_CLASS_PROPERTY, BaseConditionClass.class.getName());
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
    public FieldReference resolve(String arg, NamedType.Resolver resolver) {
        Type<?> type;
        TypeField field;
        NamedType typeRef;

        int firstDot = arg.indexOf('.');
        if (firstDot < 0) {
            // Var references type
            if ((typeRef = resolver.resolve(arg)) == null) {
                throw new IllegalArgumentException("There's no declared reference '" + arg + "' in provided context.");
            }

            type = typeRef.getType();

            field = type.getField(TypeImpl.THIS_FIELD_NAME);
            if (field == null) {
                throw new IllegalArgumentException("Type implementation doesn't support default 'this' field for " + type + ". As a workaround, use a specific type field in your expression rather than referencing the whole type.");
            }
        } else {
            // Var references field
            String lhsFactType = arg.substring(0, firstDot);
            String dottedProp = arg.substring(firstDot + 1);
            Const.assertName(dottedProp);
            Const.assertName(lhsFactType.substring(1));

            if ((typeRef = resolver.resolve(lhsFactType)) == null) {
                throw new IllegalArgumentException("There's no declared reference '" + lhsFactType + "' in provided context.");
            }

            type = typeRef.getType();
            field = type.getField(dottedProp);
            if (field == null) {
                throw new IllegalArgumentException("Unable to resolve property '" + dottedProp + "' of the type " + type);
            }
        }


        return new FieldReferenceImpl(typeRef, field);
    }

    @Override
    public synchronized Evaluator buildExpression(String rawExpression, NamedType.Resolver resolver, Set<String> imports) throws CompilationException {
        try {
            return buildExpression(rawExpression, resolver, imports, true);
        } catch (Throwable e) {
            // Trying again with the original expression. There might be a keyword like 'new',
            // which requires whitespaces to be preserved.
            return buildExpression(rawExpression, resolver, imports, false);
        }
    }

    private Evaluator buildExpression(String rawExpression, NamedType.Resolver resolver, Set<String> imports, boolean stripWhiteSpaces) throws CompilationException {
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

        try {
            Class<?> baseClass = evaluatorCompiler.getClassLoader().loadClass(conditionBaseClassName);
            return evaluatorCompiler.buildExpression(baseClass, remover, strippedExpression, terms, imports);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load class '" + conditionBaseClassName + "'");
        }
    }

}

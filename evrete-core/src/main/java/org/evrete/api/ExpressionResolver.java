package org.evrete.api;

import org.evrete.api.annotations.NonNull;
import org.evrete.runtime.compiler.CompilationException;

import java.util.Collection;
import java.util.Collections;

/**
 * An interface with a set of basic methods that are necessary for parsing string expressions.
 */
public interface ExpressionResolver {

    /**
     * <p>
     * This method takes a string as an argument and builds a {@link FieldReference} if possible.
     * </p>
     * <p>
     * For example, if the argument is "$c.category.id", the implementation might do the following:
     * </p>
     * <ol>
     *    <li>
     *        Use the supplied resolver to identify which {@link NamedType} this expression refers
     *        to (let's say it's a Customer fact, named "$c")
     *    </li>
     *    <li>
     *        Use the resolved {@link NamedType} to parse the remaining part of the argument,
     *        "category.id", by the means provided by the {@link Type} interface.
     *    </li>
     * </ol>
     *
     * @param arg      a String to parse
     * @param resolver a mapping function between fact name and full {@link NamedType}
     * @return returns {@link FieldReference} instance or throws an {@link IllegalArgumentException}
     * @throws IllegalArgumentException if the argument can not be resolved
     */
    @NonNull
    FieldReference resolve(String arg, NamedType.Resolver resolver);

    @NonNull
    default FieldReference[] resolve(@NonNull NamedType.Resolver resolver, String... strings) {
        FieldReference[] references = new FieldReference[strings.length];
        for (int i = 0; i < references.length; i++) {
            references[i] = resolve(strings[i], resolver);
        }
        return references;
    }

    /**
     * @deprecated use {@link #buildExpression(LiteralExpression)} instead
     */
    @NonNull
    @Deprecated
    default Evaluator buildExpression(String expression, NamedType.Resolver resolver) throws CompilationException {
        return buildExpression(LiteralExpression.of(expression, resolver));
    }

    /**
     * <p>
     * This method parses a string argument and returns an {@link Evaluator} if possible.
     * </p>
     *
     * @param expression - literal expression and its context
     * @return returns an {@link Evaluator} instance or throws an exception
     * @throws CompilationException     if the argument can not be compiled
     * @throws IllegalArgumentException if the expression can not be resolved
     * @throws IllegalStateException if the resolver is not in an appropriate state
     */
    @NonNull
    default Evaluator buildExpression(LiteralExpression expression) throws CompilationException {
        Collection<LiteralEvaluator> col = buildExpressions(Collections.singleton(expression));
        if(col.size() == 1) {
            return col.iterator().next();
        } else {
            throw new IllegalStateException();
        }
    }


    /**
     * <p>
     * This method builds literal expressions all at once and returns a collection of compiled results.
     * Order is not guaranteed, use the {@link LiteralEvaluator#getSource()} method to associate
     * the resulting evaluators with the method's argument list.
     * </p>
     *
     * @param expressions - literal expressions
     * @return collection of {@link LiteralEvaluator} instances
     * @throws CompilationException  if the argument can not be compiled
     * @throws IllegalArgumentException if the expression can not be resolved
     */
    Collection<LiteralEvaluator> buildExpressions(Collection<LiteralExpression> expressions) throws CompilationException;
}

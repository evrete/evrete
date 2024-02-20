package org.evrete.api;

import org.evrete.api.annotations.NonNull;

import java.util.Collection;

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
     * <p>
     * This method parses a string argument and returns an {@link Evaluator} if possible.
     * </p>
     *
     * @return returns an {@link Evaluator} instance or throws an exception
     * @throws IllegalArgumentException if the expression can not be resolved
     * @throws IllegalStateException if the resolver is not in an appropriate state
     * @deprecated in favor of {@link org.evrete.api.spi.LiteralSourceCompiler}
     */
    @NonNull
    default LiteralEvaluator buildExpression(LiteralExpression ignored) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * This method builds literal expressions all at once and returns a collection of compiled results.
     * Order is not guaranteed, use the {@link LiteralEvaluator#getSource()} method to associate
     * the resulting evaluators with the method's argument list.
     * </p>
     *
     * @return collection of {@link LiteralEvaluator} instances
     * @throws IllegalArgumentException if the expression can not be resolved
     * @deprecated in favor of {@link org.evrete.api.spi.LiteralSourceCompiler}
     */
    @Deprecated
    default Collection<LiteralEvaluator> buildExpressions(Collection<LiteralExpression> ignored) {
        throw new UnsupportedOperationException();
    }
}

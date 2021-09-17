package org.evrete.api;

import org.evrete.util.compiler.CompilationException;

import java.util.Set;

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
    FieldReference resolve(String arg, NamedType.Resolver resolver);

    default FieldReference[] resolve(NamedType.Resolver resolver, String... strings) {
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
     * @param stringExpression - a String condition expression to parse
     * @param resolver         a mapping function between fact name and {@link NamedType}
     * @param imports          a collection of class imports
     * @return returns an {@link Evaluator} instance or throws an exception
     * @throws CompilationException     if the argument can not be compiled
     * @throws IllegalArgumentException if the any parts of the argument can not be resolved
     * @see #resolve(String, NamedType.Resolver)
     */
    Evaluator buildExpression(String stringExpression, NamedType.Resolver resolver, Set<String> imports) throws CompilationException;
}

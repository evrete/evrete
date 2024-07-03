package org.evrete.api.builders;

import org.evrete.api.*;
import org.evrete.api.annotations.NonNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The LhsBuilder interface is used to build the left-hand side (LHS) of a rule.
 *
 * @param <C> the type of the {@link RuntimeContext} of the builder
 */
public interface LhsBuilder<C extends RuntimeContext<C>> {
    /**
     * <p>
     * Terminates current rule builder with the provided RHS action
     * </p>
     *
     * @param literalRhs RHS action as Java code
     * @return returns the current ruleset builder
     */
    RuleSetBuilder<C> execute(String literalRhs);

    /**
     * <p>
     * Terminates current rule builder as a rule without action.
     * </p>
     *
     * @return returns the current ruleset builder
     */
    RuleSetBuilder<C> execute();

    /**
     * <p>
     * Terminates current rule builder with the provided RHS action
     * </p>
     *
     * @param consumer RHS
     * @return returns the current ruleset builder
     */
    RuleSetBuilder<C> execute(Consumer<RhsContext> consumer);


    /**
     * Adds one or more condition expressions to the current {@link LhsBuilder}.
     *
     * @param expressions the condition expressions to add
     * @return the current {@link LhsBuilder}
     */
    default LhsBuilder<C> where(String... expressions) {
        if (expressions != null) {
            for (String expression : expressions) {
                where(expression, WorkUnit.DEFAULT_COMPLEXITY);
            }
        }
        return this;
    }

    /**
     * Adds a condition expression to the current LhsBuilder.
     *
     * @param expression the condition expression to add
     * @param complexity the complexity of the condition expression
     * @return the current {@link LhsBuilder}
     */
    LhsBuilder<C> where(@NonNull String expression, double complexity);

    /**
     * Adds a condition to the current {@link LhsBuilder} with the provided predicate, complexity, and references.
     *
     * @param predicate  the predicate to add as a condition
     * @param complexity the complexity of the condition
     * @param references the field references, e.g., <code>["$a.id", "$b.code.value", "$c"]</code>,
     *                   in the same order and of the same types as required by the predicate
     * @return the current {@link LhsBuilder}
     */
    LhsBuilder<C> where(@NonNull Predicate<Object[]> predicate, double complexity, String... references);

    /**
     * Adds a condition to the current {@link LhsBuilder} with the provided predicate,
     * default complexity, and references.
     *
     * @param predicate  the predicate to add as a condition
     * @param references the field references, e.g., <code>["$a.id", "$b.code.value", "$c"]</code>,
     *                   in the same order and of the same types as required by the predicate
     * @return the current {@link LhsBuilder}
     */
    default LhsBuilder<C> where(@NonNull Predicate<Object[]> predicate, String... references) {
        return where(predicate, WorkUnit.DEFAULT_COMPLEXITY, references);
    }

    /**
     * Adds a condition to the current {@link LhsBuilder} with the provided predicate, complexity, and field references.
     *
     * @param predicate  the predicate to add as a condition
     * @param complexity the complexity of the condition
     * @param references the field references, e.g., <code>["$a.id", "$b.code.value", "$c"]</code>,
     *                   in the same order and of the same types as required by the predicate
     * @return the current {@link LhsBuilder}
     */
    LhsBuilder<C> where(@NonNull ValuesPredicate predicate, double complexity, String... references);

    /**
     * Adds a condition to the current {@link LhsBuilder} with the provided predicate and field references.
     *
     * @param predicate  the predicate to add as a condition
     * @param references the field references, e.g., <code>["$a.id", "$b.code.value", "$c"]</code>,
     *                   in the same order and of the same types as required by the predicate
     * @return the current {@link LhsBuilder}
     */
    default LhsBuilder<C> where(@NonNull ValuesPredicate predicate, String... references) {
        return where(predicate, WorkUnit.DEFAULT_COMPLEXITY, references);
    }

    /**
     * A shorthand and fluent version of the {@link #addFactDeclaration(String, String)} method.
     *
     * @param name the name of the fact declaration, e.g. "$customer"
     * @param type the logical type of the fact, see the {@link Type} documentation on the logical types
     * @return this builder
     */
    default LhsBuilder<C> addFact(@NonNull String name, @NonNull String type) {
        addFactDeclaration(name, type);
        return this;
    }

    /**
     * Adds a fact declaration to the current LhsBuilder.
     *
     * @param name the name of the fact declaration, e.g. "$customer"
     * @param type the logical type of the fact, see the {@link Type} documentation on the logical types
     * @return the {@link NamedType} representing the fact declaration
     */
    NamedType addFactDeclaration(@NonNull String name, @NonNull String type);

    /**
     * A shorthand and fluent version of the {@link #addFactDeclaration(String, Type)} method.
     *
     * @param name the name of the fact declaration, e.g. "$customer"
     * @param type the type of the fact declaration
     * @return this builder
     */
    default LhsBuilder<C> addFact(@NonNull String name, @NonNull Type<?> type) {
        addFactDeclaration(name, type);
        return this;
    }

    /**
     * Adds a fact declaration to the current LhsBuilder.
     *
     * @param name the name of the fact declaration, e.g. "$customer"
     * @param type the type of the fact declaration
     * @return the NamedType representing the fact declaration
     * @throws NullPointerException if either the name or type parameter is null
     */
    NamedType addFactDeclaration(@NonNull String name, @NonNull Type<?> type);

    /**
     * A shorthand and fluent version of the {@link #addFactDeclaration(String, Class)} method.
     *
     * @param name the name of the fact declaration, e.g. "$customer"
     * @param type the type of the fact declaration
     * @return this builder
     */
    default LhsBuilder<C> addFact(@NonNull String name, @NonNull Class<?> type) {
        addFactDeclaration(name, type);
        return this;
    }

    /**
     * Adds a fact declaration to the current LhsBuilder.
     *
     * @param name the name of the fact declaration, e.g. "$customer"
     * @param type the type of the fact declaration
     * @return the {@link NamedType} representing the fact declaration
     */
    NamedType addFactDeclaration(@NonNull String name, @NonNull Class<?> type);
}

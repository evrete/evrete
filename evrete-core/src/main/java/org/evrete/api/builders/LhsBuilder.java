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
public interface LhsBuilder<C extends RuntimeContext<C>> extends NamedType.Resolver {
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


    default LhsBuilder<C> where(String... expressions) {
        if (expressions != null) {
            for (String expression : expressions) {
                where(expression, WorkUnit.DEFAULT_COMPLEXITY);
            }
        }
        return this;
    }

    /**
     * Adds one or more condition expressions to the current {@link LhsBuilder}.
     *
     * @param expressions the condition expressions to add
     * @return the current {@link LhsBuilder}
     */
    LhsBuilder<C> where(EvaluatorHandle... expressions);

    /**
     * Adds a condition expression to the current LhsBuilder.
     *
     * @param expression  the condition expression to add
     * @param complexity  the complexity of the condition expression
     * @return the current {@link LhsBuilder}
     */
    LhsBuilder<C> where(@NonNull String expression, double complexity);

    /**
     * Adds a condition to the current {@link LhsBuilder} with the provided predicate, complexity, and references.
     *
     * @param predicate  the predicate to add as a condition
     * @param complexity the complexity of the condition
     * @param references the references used in the condition
     * @return the current {@link LhsBuilder}
     */
    LhsBuilder<C> where(@NonNull Predicate<Object[]> predicate, double complexity, String... references);

    /**
     * Adds a condition to the current {@link LhsBuilder} with the provided predicate,
     * default complexity, and references.
     *
     * @param predicate  the predicate to add as a condition
     * @param references the references used in the condition
     * @return the current {@link LhsBuilder}
     */
    default LhsBuilder<C> where(@NonNull Predicate<Object[]> predicate, String... references) {
        return where(predicate, WorkUnit.DEFAULT_COMPLEXITY, references);
    }

    /**
     * Adds a condition to the current {@link LhsBuilder} with the provided predicate, complexity, and references.
     *
     * @param predicate  the predicate to add as a condition
     * @param complexity the complexity of the condition
     * @param references the references used in the condition
     * @return the current {@link LhsBuilder}
     */
    LhsBuilder<C> where(@NonNull ValuesPredicate predicate, double complexity, String... references);

    default LhsBuilder<C> where(@NonNull ValuesPredicate predicate, String... references) {
        return where(predicate, WorkUnit.DEFAULT_COMPLEXITY, references);
    }

    LhsBuilder<C> where(@NonNull Predicate<Object[]> predicate, double complexity, FieldReference... references);

    default LhsBuilder<C> where(@NonNull Predicate<Object[]> predicate, FieldReference... references) {
        return where(predicate, WorkUnit.DEFAULT_COMPLEXITY, references);
    }

    LhsBuilder<C> where(@NonNull ValuesPredicate predicate, double complexity, FieldReference... references);

    default LhsBuilder<C> where(@NonNull ValuesPredicate predicate, FieldReference... references) {
        return where(predicate, WorkUnit.DEFAULT_COMPLEXITY, references);
    }

    NamedType addFactDeclaration(@NonNull String name, @NonNull Type<?> type);

    NamedType addFactDeclaration(@NonNull String name, @NonNull String type);

    NamedType addFactDeclaration(@NonNull String name, @NonNull Class<?> type);
}

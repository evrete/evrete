package org.evrete.api.builders;

import org.evrete.api.*;
import org.evrete.api.annotations.NonNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

public interface LhsBuilder<C extends RuntimeContext<C>> extends NamedType.Resolver {
    /**
     * <p>
     * Terminates current rule builder with the provided RHS action
     * </p>
     *
     * @param literalRhs RHS action as Java code
     * @return context
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
     * @return context
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

    LhsBuilder<C> where(EvaluatorHandle... expressions);

    LhsBuilder<C> where(@NonNull String expression, double complexity);

    LhsBuilder<C> where(@NonNull Predicate<Object[]> predicate, double complexity, String... references);

    default LhsBuilder<C> where(@NonNull Predicate<Object[]> predicate, String... references) {
        return where(predicate, WorkUnit.DEFAULT_COMPLEXITY, references);
    }

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

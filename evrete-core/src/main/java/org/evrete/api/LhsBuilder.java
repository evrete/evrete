package org.evrete.api;

import org.evrete.api.annotations.NonNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @deprecated in favor of {@link org.evrete.api.builders.LhsBuilder}
 */
@Deprecated
public interface LhsBuilder<C extends RuntimeContext<C>> extends NamedType.Resolver {
    C execute(String literalRhs);

    RuleBuilder<C> setRhs(String literalConsumer);

    /**
     * <p>
     * Finishes LHS declaration and returns the rule builder
     * </p>
     *
     * @return rule builder
     */
    RuleBuilder<C> create();

    /**
     * <p>
     * Terminates the rule builder without RHS and adds the rule to the current context
     * </p>
     *
     * @return context
     */
    C execute();

    /**
     * <p>
     * Terminates the rule builder with the provided RHS and adds the rule to the current context
     * </p>
     *
     * @param consumer RHS
     * @return context
     */
    C execute(Consumer<RhsContext> consumer);


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

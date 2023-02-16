package org.evrete.api;

import java.util.function.Consumer;
import java.util.function.Predicate;

public interface LhsBuilder<C extends RuntimeContext<C>> extends NamedType.Resolver {
    C execute(String literalRhs);

    RuleBuilder<C> setRhs(String literalConsumer);

    /**
     * <p>
     *     Finishes LHS declaration and returns the rule builder
     * </p>
     * @return rule builder
     */
    RuleBuilder<C> create();

    /**
     * <p>
     *     Terminates the rule builder without RHS and adds the rule to the current context
     * </p>
     * @return context
     */
    C execute();

    /**
     * <p>
     *     Terminates the rule builder with the provided RHS and adds the rule to the current context
     * </p>
     * @param consumer RHS
     * @return context
     */
    C execute(Consumer<RhsContext> consumer);


    EvaluatorHandle addWhere(String expression, double complexity);

    EvaluatorHandle addWhere(ValuesPredicate predicate, double complexity, String... references);

    //TODO !!!! add javadoc with examples
    EvaluatorHandle addWhere(Predicate<Object[]> predicate, double complexity, String... references);

    EvaluatorHandle addWhere(ValuesPredicate predicate, double complexity, FieldReference... references);

    EvaluatorHandle addWhere(Predicate<Object[]> predicate, double complexity, FieldReference... references);

    default EvaluatorHandle addWhere(String expression) {
        return addWhere(expression, WorkUnit.DEFAULT_COMPLEXITY);
    }

    //TODO !!!! add javadoc with examples
    default EvaluatorHandle addWhere(ValuesPredicate predicate, String... references) {
        return addWhere(predicate, WorkUnit.DEFAULT_COMPLEXITY, references);
    }

    //TODO !!!! add javadoc with examples
    default EvaluatorHandle addWhere(Predicate<Object[]> predicate, String... references) {
        return addWhere(predicate, WorkUnit.DEFAULT_COMPLEXITY, references);
    }

    default EvaluatorHandle addWhere(ValuesPredicate predicate, FieldReference... references) {
        return addWhere(predicate, WorkUnit.DEFAULT_COMPLEXITY, references);
    }

    default EvaluatorHandle addWhere(Predicate<Object[]> predicate, FieldReference... references) {
        return addWhere(predicate, WorkUnit.DEFAULT_COMPLEXITY, references);
    }

    default LhsBuilder<C> where(String... expressions) {
        if(expressions != null) {
            for (String expression : expressions) {
                where(expression, WorkUnit.DEFAULT_COMPLEXITY);
            }
        }
        return this;
    }

    LhsBuilder<C> where(EvaluatorHandle... expressions);

    LhsBuilder<C> where(String expression, double complexity);

    LhsBuilder<C> where(Predicate<Object[]> predicate, double complexity, String... references);

    default LhsBuilder<C> where(Predicate<Object[]> predicate, String... references) {
        return where(predicate, WorkUnit.DEFAULT_COMPLEXITY, references);
    }

    LhsBuilder<C> where(ValuesPredicate predicate, double complexity, String... references);

    default LhsBuilder<C> where(ValuesPredicate predicate, String... references) {
        return where(predicate, WorkUnit.DEFAULT_COMPLEXITY, references);
    }

    LhsBuilder<C> where(Predicate<Object[]> predicate, double complexity, FieldReference... references);

    default LhsBuilder<C> where(Predicate<Object[]> predicate, FieldReference... references) {
        return where(predicate, WorkUnit.DEFAULT_COMPLEXITY, references);
    }

    LhsBuilder<C> where(ValuesPredicate predicate, double complexity, FieldReference... references);

    default LhsBuilder<C> where(ValuesPredicate predicate, FieldReference... references) {
        return where(predicate, WorkUnit.DEFAULT_COMPLEXITY, references);
    }

    NamedType addFactDeclaration(String name, Type<?> type);

    NamedType addFactDeclaration(String name, String type);

    NamedType addFactDeclaration(String name, Class<?> type);
}

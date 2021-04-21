package org.evrete.api;

import java.util.function.Consumer;
import java.util.function.Predicate;

public interface LhsBuilder<C extends RuntimeContext<C>> extends NamedType.Resolver {
    C execute(String literalRhs);

    RuleBuilder<C> setRhs(String literalConsumer);

    RuleBuilder<C> create();

    C execute();

    C execute(Consumer<RhsContext> consumer);

    RuleBuilder<C> getRuleBuilder();

    EvaluatorHandle addWhere(String expression, double complexity);

    EvaluatorHandle addWhere(ValuesPredicate predicate, double complexity, String... references);

    EvaluatorHandle addWhere(Predicate<Object[]> predicate, double complexity, String... references);

    EvaluatorHandle addWhere(ValuesPredicate predicate, double complexity, FieldReference... references);

    EvaluatorHandle addWhere(Predicate<Object[]> predicate, double complexity, FieldReference... references);

    default EvaluatorHandle addWhere(String expression) {
        return addWhere(expression, EvaluatorHandle.DEFAULT_COMPLEXITY);
    }

    default EvaluatorHandle addWhere(ValuesPredicate predicate, String... references) {
        return addWhere(predicate, EvaluatorHandle.DEFAULT_COMPLEXITY, references);
    }

    default EvaluatorHandle addWhere(Predicate<Object[]> predicate, String... references) {
        return addWhere(predicate, EvaluatorHandle.DEFAULT_COMPLEXITY, references);
    }

    default EvaluatorHandle addWhere(ValuesPredicate predicate, FieldReference... references) {
        return addWhere(predicate, EvaluatorHandle.DEFAULT_COMPLEXITY, references);
    }

    default EvaluatorHandle addWhere(Predicate<Object[]> predicate, FieldReference... references) {
        return addWhere(predicate, EvaluatorHandle.DEFAULT_COMPLEXITY, references);
    }

    LhsBuilder<C> where(String... expressions);

    LhsBuilder<C> where(EvaluatorHandle... expressions);

    LhsBuilder<C> where(String expression, double complexity);

    LhsBuilder<C> where(Predicate<Object[]> predicate, double complexity, String... references);

    LhsBuilder<C> where(Predicate<Object[]> predicate, String... references);

    LhsBuilder<C> where(ValuesPredicate predicate, double complexity, String... references);

    LhsBuilder<C> where(ValuesPredicate predicate, String... references);

    LhsBuilder<C> where(Predicate<Object[]> predicate, double complexity, FieldReference... references);

    LhsBuilder<C> where(Predicate<Object[]> predicate, FieldReference... references);

    LhsBuilder<C> where(ValuesPredicate predicate, double complexity, FieldReference... references);

    LhsBuilder<C> where(ValuesPredicate predicate, FieldReference... references);

    NamedType addFactDeclaration(String name, Type<?> type);

    NamedType addFactDeclaration(String name, String type);

    NamedType addFactDeclaration(String name, Class<?> type);
}

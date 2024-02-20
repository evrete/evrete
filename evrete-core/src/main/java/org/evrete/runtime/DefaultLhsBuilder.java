package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.builders.LhsBuilder;
import org.evrete.runtime.evaluation.EvaluatorOfArray;
import org.evrete.runtime.evaluation.EvaluatorOfPredicate;
import org.evrete.util.NamedTypeImpl;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

class DefaultLhsBuilder<C extends RuntimeContext<C>> extends DefaultTypeResolver implements LhsBuilder<C> {
    private final DefaultRuleBuilder<C> ruleBuilder;
    private final AbstractRuntime<?, C> runtime;
    private final LhsConditions conditions = new LhsConditions();

    DefaultLhsBuilder(DefaultRuleBuilder<C> ruleBuilder) {
        this.ruleBuilder = ruleBuilder;
        this.runtime = ruleBuilder.runtime();
    }

    @Override
    public synchronized NamedTypeImpl addFactDeclaration(@NonNull String name, @NonNull Type<?> type) {
        // Resetting type resolver
        NamedTypeImpl factType = new NamedTypeImpl(type, name);
        this.save(factType);
        return factType;
    }

    LhsConditions getConditions() {
        return conditions;
    }

    @Deprecated
    void copyFrom(LhsBuilderImpl<C> old) {
        super.copyFrom(old);
        this.conditions.copyFrom(old.getConditions());
    }

    @Override
    public DefaultRuleSetBuilder<C> execute(Consumer<RhsContext> consumer) {
        ruleBuilder.setRhs(consumer);
        return ruleBuilder.getRuleSetBuilder();
    }

    @Override
    public DefaultRuleSetBuilder<C> execute(String literalRhs) {
        ruleBuilder.setRhs(literalRhs);
        return ruleBuilder.getRuleSetBuilder();
    }

    @Override
    public NamedType addFactDeclaration(@NonNull String name, @NonNull Class<?> type) {
        Type<?> t = runtime.getTypeResolver().getOrDeclare(type);
        return addFactDeclaration(name, t);
    }

    @Override
    public NamedType addFactDeclaration(@NonNull String name, @NonNull String type) {
        return addFactDeclaration(name, runtime.getTypeResolver().getOrDeclare(type));
    }

    @Override
    public DefaultLhsBuilder<C> where(EvaluatorHandle... expressions) {
        if (expressions == null) return this;
        for (EvaluatorHandle expression : expressions) {
            this.conditions.add(Objects.requireNonNull(expression));
        }
        return this;
    }

    @Override
    public DefaultLhsBuilder<C> where(@NonNull String expression, double complexity) {
        whereInner(expression, complexity);
        return this;
    }

    @Override
    public DefaultLhsBuilder<C> where(@NonNull ValuesPredicate predicate, double complexity, String... references) {
        whereInner(predicate, complexity, references);
        return this;
    }

    @Override
    public DefaultLhsBuilder<C> where(@NonNull Predicate<Object[]> predicate, double complexity, FieldReference... references) {
        whereInner(predicate, complexity, references);
        return this;
    }

    @Override
    public DefaultLhsBuilder<C> where(@NonNull Predicate<Object[]> predicate, double complexity, String... references) {
        whereInner(predicate, complexity, references);
        return this;
    }

    @Override
    public DefaultLhsBuilder<C> where(@NonNull ValuesPredicate predicate, double complexity, FieldReference... references) {
        whereInner(predicate, complexity, references);
        return this;
    }

    @Override
    public DefaultRuleSetBuilder<C> execute() {
        return ruleBuilder.getRuleSetBuilder();
    }

    DefaultLhsBuilder<C> buildLhs(Collection<FactBuilder> facts) {
        if (facts == null || facts.isEmpty()) return this;
        for (FactBuilder f : facts) {
            Class<?> c = f.getResolvedType();
            if (c == null) {
                // Unresolved
                addFactDeclaration(f.getName(), f.getUnresolvedType());
            } else {
                // Resolved
                addFactDeclaration(f.getName(), c);
            }
        }
        return this;
    }

    private void whereInner(String expression, double complexity) {
        LiteralExpression literalExpression = LiteralExpression.of(Objects.requireNonNull(expression), this.ruleBuilder);
        this.conditions.add(literalExpression, complexity);
    }

    private void whereInner(ValuesPredicate predicate, double complexity, FieldReference[] references) {
        this.conditions.add(new EvaluatorOfPredicate(Objects.requireNonNull(predicate), references), complexity);
    }

    private void whereInner(Predicate<Object[]> predicate, double complexity, FieldReference[] references) {
        this.conditions.add(new EvaluatorOfArray(Objects.requireNonNull(predicate), references), complexity);
    }

    private void whereInner(ValuesPredicate predicate, double complexity, String[] references) {
        FieldReference[] descriptor = resolveFieldReferences(references);
        EvaluatorOfPredicate evaluator = new EvaluatorOfPredicate(Objects.requireNonNull(predicate), descriptor);
        this.conditions.add(evaluator, complexity);
    }

    private void whereInner(Predicate<Object[]> predicate, double complexity, String[] references) {
        FieldReference[] descriptor = resolveFieldReferences(references);
        EvaluatorOfArray evaluator = new EvaluatorOfArray(Objects.requireNonNull(predicate), descriptor);
        this.conditions.add(evaluator, complexity);
    }

    private FieldReference[] resolveFieldReferences(String[] references) {
        return runtime.resolveFieldReferences(references, DefaultLhsBuilder.this);
    }
}

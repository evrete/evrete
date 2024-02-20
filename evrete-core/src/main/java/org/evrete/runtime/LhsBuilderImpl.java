package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.api.annotations.NonNull;
import org.evrete.runtime.evaluation.EvaluatorOfArray;
import org.evrete.runtime.evaluation.EvaluatorOfPredicate;
import org.evrete.util.NamedTypeImpl;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Deprecated
class LhsBuilderImpl<C extends RuntimeContext<C>> extends  DefaultTypeResolver implements LhsBuilder<C> {
    private final RuleBuilderImpl<C> ruleBuilder;
    private final AbstractRuntime<?, C> runtime;
    private final LhsConditions conditions = new LhsConditions();


    LhsBuilderImpl(RuleBuilderImpl<C> ruleBuilder) {
        this.ruleBuilder = ruleBuilder;
        this.runtime = ruleBuilder.getRuntimeContext();
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

    @Override
    public RuleBuilder<C> create() {
        return ruleBuilder;
    }

    @Override
    public C execute(Consumer<RhsContext> consumer) {
        return ruleBuilder.build(consumer);
    }

    @Override
    public C execute(String literalRhs) {
        return ruleBuilder.build(literalRhs);
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
    public RuleBuilder<C> setRhs(String literalConsumer) {
        ruleBuilder.setRhs(literalConsumer);
        return ruleBuilder;
    }

    @Override
    public LhsBuilderImpl<C> where(EvaluatorHandle... expressions) {
        if (expressions == null) return this;
        for (EvaluatorHandle expression : expressions) {
            this.conditions.add(Objects.requireNonNull(expression));
        }
        return this;
    }

    @Override
    public LhsBuilderImpl<C> where(@NonNull String expression, double complexity) {
        whereInner(expression, complexity);
        return this;
    }

    @Override
    public LhsBuilderImpl<C> where(@NonNull ValuesPredicate predicate, double complexity, String... references) {
        whereInner(predicate, complexity, references);
        return this;
    }

    @Override
    public LhsBuilderImpl<C> where(@NonNull Predicate<Object[]> predicate, double complexity, FieldReference... references) {
        whereInner(predicate, complexity, references);
        return this;
    }

    @Override
    public LhsBuilderImpl<C> where(@NonNull Predicate<Object[]> predicate, double complexity, String... references) {
        whereInner(predicate, complexity, references);
        return this;
    }

    @Override
    public LhsBuilderImpl<C> where(@NonNull ValuesPredicate predicate, double complexity, FieldReference... references) {
        whereInner(predicate, complexity, references);
        return this;
    }

    @Override
    public C execute() {
        return ruleBuilder.build();
    }

    LhsBuilderImpl<C> buildLhs(Collection<FactBuilder> facts) {
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
        return runtime.resolveFieldReferences(references, LhsBuilderImpl.this);
    }
}

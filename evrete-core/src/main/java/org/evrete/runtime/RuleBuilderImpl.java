package org.evrete.runtime;

import org.evrete.AbstractRule;
import org.evrete.api.*;
import org.evrete.api.annotations.NonNull;
import org.evrete.runtime.evaluation.EvaluatorOfArray;
import org.evrete.runtime.evaluation.EvaluatorOfPredicate;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

class RuleBuilderImpl<C extends RuntimeContext<C>> extends AbstractRule implements RuleBuilder<C> {
    public static final int NULL_SALIENCE = Integer.MIN_VALUE;
    private final AbstractRuntime<?, C> runtime;
    private final LhsBuilderImpl<C> lhsBuilder;

    RuleBuilderImpl(AbstractRuntime<?, C> ctx, String name) {
        super(name, NULL_SALIENCE);
        this.runtime = ctx;
        this.lhsBuilder = new LhsBuilderImpl<>(this);
    }

    @Override
    public RuleBuilderImpl<C> set(String property, Object value) {
        super.set(property, value);
        return this;
    }

    @Override
    public RuleBuilderImpl<C> salience(int salience) {
        setSalience(salience);
        return this;
    }

    @Override
    @NonNull
    public NamedType resolve(@NonNull String var) {
        return lhsBuilder.resolve(var);
    }

    @Override
    public <Z> RuleBuilder<C> property(String property, Z value) {
        this.set(property, value);
        return this;
    }

    @Override
    public LhsBuilderImpl<C> getLhs() {
        return lhsBuilder;
    }

    C build() {
        runtime.addRule(this);
        return getRuntime();
    }

    @SuppressWarnings("unchecked")
    @Override
    public C getRuntime() {
        return (C) runtime;
    }

    C build(Consumer<RhsContext> rhs) {
        setRhs(rhs);
        return build();
    }

    C build(String literalRhs) {
        setRhs(literalRhs);
        return build();
    }

    @Override
    public LhsBuilderImpl<C> forEach(Collection<FactBuilder> facts) {
        return lhsBuilder.buildLhs(facts);
    }

    AbstractRuntime<?, C> getRuntimeContext() {
        return runtime;
    }

    @Override
    public EvaluatorHandle createCondition(ValuesPredicate predicate, double complexity, FieldReference... references) {
        return runtime.addEvaluator(new EvaluatorOfPredicate(predicate, references), complexity);
    }

    @Override
    public EvaluatorHandle createCondition(Predicate<Object[]> predicate, double complexity, FieldReference... references) {
        return runtime.addEvaluator(new EvaluatorOfArray(predicate, references), complexity);
    }

    @Override
    public EvaluatorHandle createCondition(String expression, double complexity) {
        Evaluator evaluator = runtime.compileUnchecked(expression, this);
        return runtime.addEvaluator(evaluator, complexity);
    }

    @Override
    public EvaluatorHandle createCondition(ValuesPredicate predicate, double complexity, String... references) {
        return createCondition(predicate, complexity, resolveFieldReferences(references));
    }

    @Override
    public EvaluatorHandle createCondition(Predicate<Object[]> predicate, double complexity, String... references) {
        return createCondition(predicate, complexity, resolveFieldReferences(references));
    }

    private FieldReference[] resolveFieldReferences(String[] references) {
        return runtime.resolveFieldReferences(references, this);
    }

}

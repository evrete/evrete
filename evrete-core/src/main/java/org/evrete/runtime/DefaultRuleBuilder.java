package org.evrete.runtime;

import org.evrete.AbstractRule;
import org.evrete.api.*;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.builders.RuleBuilder;
import org.evrete.runtime.compiler.CompilationException;
import org.evrete.runtime.evaluation.EvaluatorOfArray;
import org.evrete.runtime.evaluation.EvaluatorOfPredicate;

import java.util.Collection;
import java.util.function.Predicate;

class DefaultRuleBuilder<C extends RuntimeContext<C>> extends AbstractRule implements RuleBuilder<C>, LhsConditionsHolder {
    private final DefaultLhsBuilder<C> lhsBuilder;

    private final DefaultRuleSetBuilder<C> ruleSetBuilder;

    DefaultRuleBuilder(DefaultRuleSetBuilder<C> ruleSetBuilder, String name) {
        super(name);
        this.ruleSetBuilder = ruleSetBuilder;
        this.lhsBuilder = new DefaultLhsBuilder<>(this);
    }

    DefaultRuleSetBuilder<C> getRuleSetBuilder() {
        return ruleSetBuilder;
    }

    String literalRhs() {
        return super.getLiteralRhs();
    }

    @Override
    public LhsConditions getConditions() {
        return lhsBuilder.getConditions();
    }

    @Override
    public Collection<NamedType> getDeclaredFactTypes() {
        return lhsBuilder.getDeclaredFactTypes();
    }

    @Override
    public DefaultRuleBuilder<C> set(String property, Object value) {
        super.set(property, value);
        return this;
    }

    @Override
    public DefaultRuleBuilder<C> salience(int salience) {
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
    public DefaultLhsBuilder<C> getLhs() {
        return lhsBuilder;
    }

    @SuppressWarnings("unchecked")
    @Override
    public C getRuntime() {
        return (C) runtime();
    }

    @Deprecated
    void copyFrom(RuleBuilderImpl<C> old) {
        // 1. Name & salience
        this.salience(old.getSalience());
        this.setName(old.getName());

        // 2. LHS
        this.lhsBuilder.copyFrom(old.getLhs());

        // 3. RHS
        String literalRhs = old.getLiteralRhs();
        if (literalRhs != null) {
            this.setRhs(literalRhs);
        } else {
            this.setRhs(old.getRhs());
        }
    }


    @Override
    public DefaultLhsBuilder<C> forEach(Collection<FactBuilder> facts) {
        return lhsBuilder.buildLhs(facts);
    }

    AbstractRuntime<?, C> runtime() {
        return ruleSetBuilder.getRuntime();
    }

    @Override
    public EvaluatorHandle createCondition(ValuesPredicate predicate, double complexity, FieldReference... references) {
        return runtime().addEvaluator(new EvaluatorOfPredicate(predicate, references), complexity);
    }

    @Override
    public EvaluatorHandle createCondition(Predicate<Object[]> predicate, double complexity, FieldReference... references) {
        return runtime().addEvaluator(new EvaluatorOfArray(predicate, references), complexity);
    }

    @Override
    public EvaluatorHandle createCondition(String expression, double complexity) throws CompilationException {
        Evaluator evaluator = runtime().compile(LiteralExpression.of(expression, this));
        return runtime().addEvaluator(evaluator, complexity);
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
        return runtime().resolveFieldReferences(references, this);
    }

}

package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.builders.LhsBuilder;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

class DefaultLhsBuilder<C extends RuntimeContext<C>> extends DefaultNamedTypeResolver<DefaultLhsBuilder.Fact> implements LhsBuilder<C> {
    private final DefaultRuleBuilder<C> ruleBuilder;
    private final AbstractRuntime<?, C> runtime;
    private final DefaultConditionManager conditionManager;

    DefaultLhsBuilder(DefaultRuleBuilder<C> ruleBuilder) {
        this.ruleBuilder = ruleBuilder;
        this.runtime = ruleBuilder.runtime();
        this.conditionManager = new DefaultConditionManager(ruleBuilder.runtime(), this);
    }

    DefaultConditionManager getConditionManager() {
        return conditionManager;
    }

    @Override
    public synchronized Fact addFactDeclaration(@NonNull String name, @NonNull Type<?> type) {
        Fact newFact = new Fact(super.size(), name, type);
        super.save(newFact);
        return newFact;
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
    public DefaultLhsBuilder<C> where(@NonNull Predicate<Object[]> predicate, double complexity, String... references) {
        whereInner(predicate, complexity, references);
        return this;
    }

    @Override
    public DefaultRuleSetBuilder<C> execute() {
        return ruleBuilder.execute();
    }

    @Override
    public DefaultRuleSetBuilder<C> execute(Consumer<RhsContext> consumer) {
        return ruleBuilder.execute(consumer);
    }

    @Override
    public DefaultRuleSetBuilder<C> execute(String literalRhs) {
        return ruleBuilder.execute(literalRhs);
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
        this.conditionManager.addLhsBuilderCondition(expression, complexity);
    }

    private void whereInner(ValuesPredicate predicate, double complexity, String[] references) {
        this.conditionManager.addLhsBuilderCondition(predicate, complexity, references);
    }

    private void whereInner(Predicate<Object[]> predicate, double complexity, String[] references) {
        this.conditionManager.addLhsBuilderCondition(predicate, complexity, references);
    }

    static class Fact extends AbstractLhsFact implements NamedType {
        final Type<?> type;

        public Fact(int inRuleIndex, String varName, Type<?> type) {
            super(inRuleIndex, varName);
            this.type = type;
        }

        @Override
        public Type<?> getType() {
            return type;
        }
    }
}

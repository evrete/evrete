package org.evrete.runtime;

import org.evrete.api.RuntimeContext;
import org.evrete.api.builders.RuleBuilder;
import org.evrete.api.builders.RuleSetBuilder;
import org.evrete.util.AbstractEnvironment;

import java.util.ArrayList;
import java.util.List;

class DefaultRuleSetBuilder<C extends RuntimeContext<C>> extends AbstractEnvironment implements RuleSetBuilder<C> {
    private final AbstractRuntime<?, C> runtime;
    private final List<DefaultRuleBuilder<C>> ruleBuilders = new ArrayList<>();
    private boolean open = true;

    DefaultRuleSetBuilder(AbstractRuntime<?, C> runtime) {
        super(runtime);
        this.runtime = runtime;
    }

    @Override
    public DefaultRuleBuilder<C> newRule(String name) {
        this.assertOpen();
        DefaultRuleBuilder<C> ruleBuilder = new DefaultRuleBuilder<>(this, name);
        this.ruleBuilders.add(ruleBuilder);
        return ruleBuilder;
    }

    @Override
    public RuleSetBuilder<C> set(String property, Object value) {
        this.assertOpen();
        super.set(property, value);
        return this;
    }

    @Override
    public final RuleBuilder<C> newRule() {
        return newRule(runtime.unnamedRuleName());
    }

    @Override
    public C build() {
        this.assertOpen();
        runtime.addRules(this);
        this.open = false;
        return getContext();
    }

    private void assertOpen() {
        if (!this.open) {
            throw new IllegalStateException("This ruleset has not been built.");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public C getContext() {
        return (C) runtime;
    }

    List<DefaultRuleBuilder<C>> getRuleBuilders() {
        return ruleBuilders;
    }

    AbstractRuntime<?, C> getRuntime() {
        return runtime;
    }
}

package org.evrete.runtime;

import org.evrete.api.RuntimeContext;
import org.evrete.api.builders.RuleBuilder;
import org.evrete.api.builders.RuleSetBuilder;

import java.util.ArrayList;
import java.util.List;

class DefaultRuleSetBuilder<C extends RuntimeContext<C>> implements RuleSetBuilder<C> {
    private final AbstractRuntime<?, C> runtime;
    private final List<DefaultRuleBuilder<C>> ruleBuilders = new ArrayList<>();

    DefaultRuleSetBuilder(AbstractRuntime<?, C> runtime) {
        this.runtime = runtime;
    }

    @Override
    public DefaultRuleBuilder<C> newRule(String name) {
        DefaultRuleBuilder<C> ruleBuilder = new DefaultRuleBuilder<>(this, name);
        this.ruleBuilders.add(ruleBuilder);
        return ruleBuilder;
    }

    @Override
    public final RuleBuilder<C> newRule() {
        return newRule(runtime.unnamedRuleName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public C build() {
        runtime.addRules(this.ruleBuilders);
        return (C) runtime;
    }

    AbstractRuntime<?, C> getRuntime() {
        return runtime;
    }
}

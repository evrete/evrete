package org.evrete.runtime;

import org.evrete.api.RuleLiteralData;
import org.evrete.api.annotations.NonNull;

import java.util.Collection;
import java.util.LinkedList;

class DefaultRuleLiteralData implements RuleLiteralData<DefaultRuleBuilder<?>, DefaultConditionManager.Literal> {
    private final DefaultRuleBuilder<?> ruleBuilder;
    private final Collection<DefaultConditionManager.Literal> conditions;

    private final String rhs;

    public DefaultRuleLiteralData(DefaultRuleBuilder<?> ruleBuilder) {
        this.ruleBuilder = ruleBuilder;
        this.conditions = new LinkedList<>();

        // Copy literal conditions
        this.conditions.addAll(ruleBuilder.getConditionManager().getLiterals());

        // Get RHS
        this.rhs = ruleBuilder.literalRhs();
    }

    boolean nonEmpty() {
        return this.rhs != null || !this.conditions.isEmpty();
    }

    @NonNull
    @Override
    public DefaultRuleBuilder<?> getRule() {
        return ruleBuilder;
    }

    @NonNull
    @Override
    public Collection<DefaultConditionManager.Literal> conditions() {
        return conditions;
    }

    @Override
    public String rhs() {
        return rhs;
    }
}

package org.evrete.runtime;

import org.evrete.api.RuleLiteralData;
import org.evrete.util.WorkUnitObject;

import java.util.Collection;
import java.util.LinkedList;

class DefaultRuleLiteralData implements RuleLiteralData<DefaultRuleBuilder<?>> {
    private final DefaultRuleBuilder<?> ruleBuilder;
    private final Collection<String> conditions;

    private final String rhs;

    public DefaultRuleLiteralData(DefaultRuleBuilder<?> ruleBuilder) {
        this.ruleBuilder = ruleBuilder;
        this.conditions = new LinkedList<>();

        // Copy literal conditions
        for (WorkUnitObject<String> condition : ruleBuilder.getConditions().literals) {
            this.conditions.add(condition.getDelegate());
        }

        // Get RHS
        this.rhs = ruleBuilder.literalRhs();
    }

    boolean nonEmpty() {
        return this.rhs != null || !this.conditions.isEmpty();
    }

    @Override
    public DefaultRuleBuilder<?> getRule() {
        return ruleBuilder;
    }

    @Override
    public Collection<String> conditions() {
        return conditions;
    }

    @Override
    public String rhs() {
        return rhs;
    }
}

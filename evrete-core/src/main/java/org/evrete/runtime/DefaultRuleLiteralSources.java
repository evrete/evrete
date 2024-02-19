package org.evrete.runtime;

import org.evrete.api.LiteralExpression;
import org.evrete.api.Rule;
import org.evrete.api.RuleLiteralSources;
import org.evrete.util.WorkUnitObject;

import java.util.Collection;
import java.util.LinkedList;

class DefaultRuleLiteralSources implements RuleLiteralSources<DefaultRuleBuilder<?>> {
    private final DefaultRuleBuilder<?> ruleBuilder;
    private final Collection<LiteralExpression> conditions;

    private final LiteralExpression rhs;

    public DefaultRuleLiteralSources(DefaultRuleBuilder<?> ruleBuilder) {
        this.ruleBuilder = ruleBuilder;
        this.conditions = new LinkedList<>();

        // Copy literal conditions
        for(WorkUnitObject<LiteralExpression> condition : ruleBuilder.getConditions().literals) {
            this.conditions.add(condition.getDelegate());
        }

        // Get RHS
        String rhs = ruleBuilder.literalRhs();
        this.rhs = rhs == null ? null : new LiteralExpression() {
            @Override
            public String getSource() {
                throw new UnsupportedOperationException("Not implemented");
            }

            @Override
            public Rule getContext() {
                return ruleBuilder;
            }
        };
    }

    boolean nonEmpty() {
        return this.rhs != null || !this.conditions.isEmpty();
    }

    @Override
    public DefaultRuleBuilder<?> getRule() {
        return ruleBuilder;
    }

    @Override
    public Collection<LiteralExpression> conditions() {
        return conditions;
    }

    @Override
    public LiteralExpression rhs() {
        return rhs;
    }
}

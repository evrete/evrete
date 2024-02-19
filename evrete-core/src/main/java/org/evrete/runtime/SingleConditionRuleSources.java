package org.evrete.runtime;

import org.evrete.api.LiteralExpression;
import org.evrete.api.Rule;
import org.evrete.api.RuleLiteralSources;
import org.evrete.util.WorkUnitObject;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

class SingleConditionRuleSources implements RuleLiteralSources<Rule> {
    private final LiteralExpression expression;


    public SingleConditionRuleSources(LiteralExpression expression) {
        this.expression = expression;
    }

    @Override
    public Rule getRule() {
        return expression.getContext();
    }

    @Override
    public Collection<LiteralExpression> conditions() {
        return Collections.singleton(expression);
    }

    @Override
    public LiteralExpression rhs() {
        return null;
    }




}

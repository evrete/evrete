package org.evrete.runtime;

import org.evrete.api.LiteralExpression;
import org.evrete.api.Rule;
import org.evrete.api.RuleLiteralSources;

import java.util.Collection;
import java.util.Collections;

class JustRhsRuleSources implements RuleLiteralSources<Rule> {
    private final LiteralExpression rhs;

    public JustRhsRuleSources(LiteralExpression rhs) {
        this.rhs = rhs;
    }

    @Override
    public Rule getRule() {
        return rhs.getContext();
    }

    @Override
    public Collection<LiteralExpression> conditions() {
        return Collections.emptyList();
    }

    @Override
    public LiteralExpression rhs() {
        return rhs;
    }


}

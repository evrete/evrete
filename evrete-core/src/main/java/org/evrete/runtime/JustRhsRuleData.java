package org.evrete.runtime;

import org.evrete.api.LiteralExpression;
import org.evrete.api.Rule;
import org.evrete.api.RuleLiteralData;

import java.util.Collection;
import java.util.Collections;

class JustRhsRuleData implements RuleLiteralData<Rule> {
    private final LiteralExpression rhs;

    public JustRhsRuleData(LiteralExpression rhs) {
        this.rhs = rhs;
    }

    @Override
    public Rule getRule() {
        return rhs.getContext();
    }

    @Override
    public Collection<String> conditions() {
        return Collections.emptyList();
    }

    @Override
    public String rhs() {
        return rhs.getSource();
    }


}

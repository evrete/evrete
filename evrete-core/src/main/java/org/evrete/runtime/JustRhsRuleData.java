package org.evrete.runtime;

import org.evrete.api.LiteralPredicate;
import org.evrete.api.Rule;
import org.evrete.api.RuleLiteralData;
import org.evrete.api.annotations.NonNull;

import java.util.Collection;
import java.util.Collections;

class JustRhsRuleData implements RuleLiteralData<Rule, LiteralPredicate> {
    private final String rhs;
    private final Rule rule;

    public JustRhsRuleData(String rhs, Rule rule) {
        this.rhs = rhs;
        this.rule = rule;
    }

    @NonNull
    @Override
    public Rule getRule() {
        return rule;
    }

    @NonNull
    @Override
    public Collection<LiteralPredicate> conditions() {
        return Collections.emptyList();
    }

    @Override
    public String rhs() {
        return rhs;
    }


}

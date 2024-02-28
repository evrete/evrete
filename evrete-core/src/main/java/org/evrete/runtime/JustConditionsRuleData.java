package org.evrete.runtime;

import org.evrete.api.Rule;
import org.evrete.api.RuleLiteralData;
import org.evrete.api.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;

class JustConditionsRuleData implements RuleLiteralData<Rule> {
    private final Collection<String> expressions;
    private final Rule rule;


    public JustConditionsRuleData(Rule rule, Collection<String> expressions) {
        this.expressions = expressions;
        this.rule = rule;
    }

    public JustConditionsRuleData(Rule rule) {
        this(rule, new ArrayList<>());
    }

    @NonNull
    @Override
    public Rule getRule() {
        return rule;
    }

    @NonNull
    @Override
    public Collection<String> conditions() {
        return expressions;
    }

    @Override
    public String rhs() {
        return null;
    }




}

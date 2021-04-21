package org.evrete.runtime;

import org.evrete.util.SearchList;

public class RuntimeRules extends SearchList<RuntimeRuleImpl> {

    RuntimeRuleImpl addRule(RuleDescriptor ruleDescriptor, AbstractRuleSession<?> session) {
        RuntimeRuleImpl r = new RuntimeRuleImpl(ruleDescriptor, session);
        this.add(r);
        return r;
    }
}

package org.evrete.runtime;

class RuntimeRules extends SearchList<RuntimeRuleImpl> {

    RuntimeRuleImpl addRule(RuleDescriptorImpl ruleDescriptor, AbstractRuleSession<?> session) {
        RuntimeRuleImpl r = new RuntimeRuleImpl(ruleDescriptor, session);
        this.add(r);
        return r;
    }
}

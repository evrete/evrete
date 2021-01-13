package org.evrete.runtime;

import org.evrete.api.Rule;
import org.evrete.api.RuntimeRule;
import org.evrete.runtime.memory.SessionMemory;

import java.util.*;

public class RuntimeRules implements Iterable<RuntimeRuleImpl> {
    private final List<RuntimeRuleImpl> list = new ArrayList<>();
    private final SessionMemory runtime;

    public RuntimeRules(SessionMemory runtime) {
        this.runtime = runtime;
    }

    private void add(RuntimeRuleImpl rule) {
        this.list.add(rule);
    }

    public RuntimeRuleImpl addRule(RuleDescriptor ruleDescriptor) {
        RuntimeRuleImpl r = new RuntimeRuleImpl(ruleDescriptor, runtime);
        this.add(r);
        return r;
    }

    public void sort(Comparator<Rule> comparator) {
        this.list.sort(comparator);
    }

    @Override
    public Iterator<RuntimeRuleImpl> iterator() {
        return list.iterator();
    }

    public List<RuntimeRule> asList() {
        return Collections.unmodifiableList(list);
    }

}

package org.evrete.runtime;

import org.evrete.runtime.memory.SessionMemory;
import org.evrete.runtime.structure.RuleDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class RuntimeRules implements Iterable<RuntimeRule>, MemoryChangeListener {
    private final List<RuntimeRule> list = new ArrayList<>();
    private final Collection<RuntimeAggregateLhsJoined> aggregateLhsGroups = new ArrayList<>();
    private final SessionMemory runtime;

    public RuntimeRules(SessionMemory runtime) {
        this.runtime = runtime;
    }

    private void add(RuntimeRule rule) {
        this.list.add(rule);
        this.aggregateLhsGroups.addAll(rule.getAggregateLhsGroups());
    }

    public RuntimeRule addRule(RuleDescriptor ruleDescriptor) {
        RuntimeRule r = new RuntimeRule(ruleDescriptor, runtime);
        this.add(r);
        return r;
    }

    public Collection<RuntimeAggregateLhsJoined> getAggregateLhsGroups() {
        return aggregateLhsGroups;
    }

    @Override
    public Iterator<RuntimeRule> iterator() {
        return list.iterator();
    }

    public List<RuntimeRule> asList() {
        return list;
    }

    @Override
    public void onAfterChange() {
        for (RuntimeRule rule : list) {
            rule.onAfterChange();
        }
    }
}

package org.evrete.runtime;

import org.evrete.api.Action;
import org.evrete.api.Rule;
import org.evrete.api.RuntimeRule;
import org.evrete.runtime.async.*;
import org.evrete.runtime.memory.BetaEndNode;
import org.evrete.runtime.memory.SessionMemory;

import java.util.*;

public class RuntimeRules implements Iterable<RuntimeRuleImpl> {
    private final List<RuntimeRuleImpl> list = new ArrayList<>();
    private final Collection<RuntimeAggregateLhsJoined> aggregateLhsGroups = new ArrayList<>();
    private final SessionMemory runtime;


    public RuntimeRules(SessionMemory runtime) {
        this.runtime = runtime;
    }

    private void add(RuntimeRuleImpl rule) {
        this.list.add(rule);
        this.aggregateLhsGroups.addAll(rule.getLhs().getAggregateConditionedGroups());
    }

    public RuntimeRuleImpl addRule(RuleDescriptor ruleDescriptor) {
        RuntimeRuleImpl r = new RuntimeRuleImpl(ruleDescriptor, runtime);
        this.add(r);
        return r;
    }

    public void sort(Comparator<Rule> comparator) {
        this.list.sort(comparator);
    }

    public Collection<RuntimeAggregateLhsJoined> getAggregateLhsGroups() {
        return aggregateLhsGroups;
    }

    @Override
    public Iterator<RuntimeRuleImpl> iterator() {
        return list.iterator();
    }

    public List<RuntimeRule> asList() {
        return Collections.unmodifiableList(list);
    }

    public List<RuntimeRule> activeRules() {
        List<RuntimeRule> l = new LinkedList<>();
        for (RuntimeRuleImpl rule : list) {
            if (rule.isInActiveState()) {
                l.add(rule);
            }
        }
        return l;
    }

    public void updateBetaMemories(Action... actions) {

        List<Completer> tasks = new ArrayList<>(list.size() * 2);

        for (Action action : actions) {
            switch (action) {
                case INSERT:
                    // Ordered task 1 - update end nodes
                    Collection<BetaEndNode> deltaEndNodes = new LinkedList<>();

                    for (RuntimeRuleImpl rule : list) {
                        for (BetaEndNode endNode : rule.getLhs().getAllBetaEndNodes()) {
                            if (endNode.hasDeltaSources()) {
                                deltaEndNodes.add(endNode);
                            }
                        }
                    }

                    if (!deltaEndNodes.isEmpty()) {
                        tasks.add(new RuleMemoryInsertTask(deltaEndNodes, true));
                    }

                    // Ordered task 2 - update aggregate nodes
                    Collection<RuntimeAggregateLhsJoined> aggregateGroups = getAggregateLhsGroups();
                    if (!aggregateGroups.isEmpty()) {
                        tasks.add(new AggregateComputeTask(aggregateGroups, true));
                    }
                    break;
                case RETRACT:
                    // Delete async tasks
                    Collection<RuntimeRuleImpl> ruleDeleteChanges = new LinkedList<>();
                    for (RuntimeRuleImpl rule : list) {
                        if (rule.isDeleteDeltaAvailable()) {
                            ruleDeleteChanges.add(rule);
                        }
                    }
                    if (!ruleDeleteChanges.isEmpty()) {
                        tasks.add(new RuleMemoryDeleteTask(ruleDeleteChanges));
                    }
                    break;
                default:
                    throw new IllegalStateException();


            }
        }


        if (tasks.size() > 0) {
            ForkJoinExecutor executor = runtime.getExecutor();
            for (Completer task : tasks) {
                executor.invoke(task);
            }
        }

    }
}

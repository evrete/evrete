package org.evrete.runtime;

import org.evrete.api.Rule;
import org.evrete.api.RuntimeRule;
import org.evrete.api.Type;
import org.evrete.runtime.async.AggregateComputeTask;
import org.evrete.runtime.async.Completer;
import org.evrete.runtime.async.ForkJoinExecutor;
import org.evrete.runtime.async.RuleMemoryInsertTask;
import org.evrete.runtime.memory.BetaEndNode;
import org.evrete.runtime.memory.SessionMemory;
import org.evrete.runtime.memory.TypeMemory;

import java.util.*;

public class RuntimeRules implements Iterable<RuntimeRuleImpl> {
    private final List<RuntimeRuleImpl> list = new ArrayList<>();
    private final Collection<RuntimeAggregateLhsJoined> aggregateLhsGroups = new ArrayList<>();
    private final SessionMemory runtime;
    //private final Agenda agenda;

    public RuntimeRules(SessionMemory runtime) {
        this.runtime = runtime;
        //this.agenda = new Agenda(list);
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

/*
    public Agenda activeRules() {
        return agenda.update();
    }
*/


    public List<RuntimeRule> agenda() {
        throw new UnsupportedOperationException();
    }

/*
    //TODO !!!! optimize
    public void propagateDeleteChanges(Collection<TypeMemory> memories) {
        MapOfList<RuntimeRuleImpl, Type<?>> affectedRules = new MapOfList<>();
        for (RuntimeRuleImpl rule : this.list) {
            for (TypeMemory tm : memories) {
                if (rule.dependsOn(tm.getType())) {
                    affectedRules.add(rule, tm.getType());
                }
            }
        }

        if (!affectedRules.isEmpty()) {
            runtime.getExecutor().invoke(new RuleMemoryDeleteTask(affectedRules));
        }
    }
*/

    //TODO !!!! optimize
    public List<RuntimeRule> propagateInsertChanges(Collection<TypeMemory> memories) {
        // Build beta-deltas
        for (TypeMemory tm : memories) {
            tm.propagateBetaDeltas();
        }


        List<RuntimeRule> affectedRules = new LinkedList<>();
        Set<BetaEndNode> affectedEndNodes = new HashSet<>();
        // Scanning rules first because they are sorted by salience
        for (RuntimeRuleImpl rule : this.list) {
            rule.mergeNodeDeltas();
            boolean ruleAdded = false;

            for (TypeMemory tm : memories) {
                Type<?> t = tm.getType();
                if (!ruleAdded && rule.dependsOn(t)) {
                    affectedRules.add(rule);
                    ruleAdded = true;
                }

                for (BetaEndNode endNode : rule.getLhs().getAllBetaEndNodes()) {
                    if (endNode.dependsOn(t)) {
                        affectedEndNodes.add(endNode);
                    }
                }
            }
        }

        // Ordered task 1 - process beta nodes, i.e. evaluate conditions
        List<Completer> tasks = new LinkedList<>();
        if (!affectedEndNodes.isEmpty()) {
            tasks.add(new RuleMemoryInsertTask(affectedEndNodes, true));
        }

        // Ordered task 2 - update aggregate nodes
        Collection<RuntimeAggregateLhsJoined> aggregateGroups = getAggregateLhsGroups();
        if (!aggregateGroups.isEmpty()) {
            tasks.add(new AggregateComputeTask(aggregateGroups, true));
        }

        if (tasks.size() > 0) {
            ForkJoinExecutor executor = runtime.getExecutor();
            for (Completer task : tasks) {
                executor.invoke(task);
            }
        }


        return affectedRules;
    }

/*
    public void updateBetaMemories(Action... actions) {
        if (actions == null || actions.length == 0) throw new IllegalStateException();


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
*/
}

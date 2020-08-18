package org.evrete.runtime.async;

import org.evrete.api.KeysStore;
import org.evrete.runtime.FactType;
import org.evrete.runtime.RuntimeAggregateLhsJoined;
import org.evrete.runtime.RuntimeFactType;
import org.evrete.runtime.RuntimeRuleImpl;
import org.evrete.runtime.memory.BetaEndNode;
import org.evrete.util.Bits;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CountedCompleter;

//import org.evrete.runtime.memory.AggregateJoinConditionNode;

public class BetaMemoryDeleteTask extends Completer {
    //private final Collection<BetaEndNode> betaEndNodes;
    private final RuntimeRuleImpl rule;

    public BetaMemoryDeleteTask(Completer parent, RuntimeRuleImpl rule) {
        super(parent);
        //this.betaEndNodes = rule.getNodesToDelete();
        this.rule = rule;
    }

    @Override
    protected void execute() {

        Bits deleteMask = new Bits();
        Collection<BetaEndNode> betaEndNodes = new HashSet<>();
        for (BetaEndNode endNode : rule.getAllBetaEndNodes()) {
            for (RuntimeFactType factType : endNode.getEntryNodes()) {
                if (factType.isDeleteDeltaAvailable()) {
                    deleteMask.set(factType.getInRuleIndex());
                    betaEndNodes.add(endNode);
                }
            }
        }


        tailCall(
                betaEndNodes,
                g -> new NodeCleanTask(BetaMemoryDeleteTask.this, g, rule, deleteMask)
        );
    }

    @Override
    public void onCompletion(CountedCompleter<?> caller) {
        Collection<RuntimeAggregateLhsJoined> aggregateNodes = rule.getAggregateLhsGroups();
        if (aggregateNodes.isEmpty()) return;


        Collection<Runnable> aggregateTasks = new ArrayList<>(aggregateNodes.size());
        for (RuntimeAggregateLhsJoined node : aggregateNodes) {
            aggregateTasks.add(() -> {
                KeysStore subject = node.getSuccessData();
                FactType[][] grouping = node.getDescriptor().getJoinCondition().getGrouping();
                ValueRowPredicate[] predicates = ValueRowPredicate.predicates(grouping, rule.getDeletedKeys());
                subject.delete(predicates);
            });

            Completer.of(aggregateTasks).invoke();
        }
    }
}

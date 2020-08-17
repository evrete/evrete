package org.evrete.runtime.async;

import org.evrete.runtime.RuntimeAggregateLhsJoined;
import org.evrete.runtime.RuntimeRule;
import org.evrete.runtime.memory.BetaEndNode;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountedCompleter;

public class RuleHotDeploymentTask extends Completer {
    private final RuntimeRule rule;

    public RuleHotDeploymentTask(RuntimeRule rule) {
        this.rule = rule;
    }

    @Override
    protected void execute() {
        // Terminal nodes for each beta graph should be evaluated
        List<NodeDeltaTask> deltaTasks = new LinkedList<>();
        for (BetaEndNode endNode : rule.getAllBetaEndNodes()) {
            NodeDeltaTask dt = new NodeDeltaTask(this, endNode, false);
            deltaTasks.add(dt);
        }
        Iterator<NodeDeltaTask> it = deltaTasks.iterator();
        while (it.hasNext()) {
            NodeDeltaTask c = it.next();
            addToPendingCount(1);
            if (it.hasNext()) {
                c.fork();
            } else {
                // Execute the tail in current thread
                c.compute();
            }
        }
    }

    @Override
    // Compute this node's delta
    public void onCompletion(CountedCompleter<?> caller) {
        //Merge terminal nodes' data
        for (BetaEndNode endNode : rule.getAllBetaEndNodes()) {
            endNode.mergeDelta();
        }

        // Evaluate aggregate nodes if any
        for (RuntimeAggregateLhsJoined agg : rule.getAggregateLhsGroups()) {
            agg.evaluate(false);
        }
    }

}

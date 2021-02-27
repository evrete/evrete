package org.evrete.runtime.async;

import org.evrete.runtime.BetaEndNode;
import org.evrete.runtime.RuntimeRuleImpl;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountedCompleter;

public class RuleHotDeploymentTask extends Completer {
    private final RuntimeRuleImpl rule;

    public RuleHotDeploymentTask(RuntimeRuleImpl rule) {
        this.rule = rule;
    }

    @Override
    protected void execute() {
        // Terminal nodes for each beta graph should be evaluated
        List<NodeDeltaTask> deltaTasks = new LinkedList<>();
        for (BetaEndNode endNode : rule.getLhs().getAllBetaEndNodes()) {
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
    public void onCompletion(CountedCompleter<?> caller) {
        // Merging nodes' deltas
        for (BetaEndNode endNode : rule.getLhs().getAllBetaEndNodes()) {
            endNode.commitDelta();
        }
    }

}

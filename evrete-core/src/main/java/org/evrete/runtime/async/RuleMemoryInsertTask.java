package org.evrete.runtime.async;

import org.evrete.runtime.FireContext;
import org.evrete.runtime.RuntimeRule;
import org.evrete.runtime.memory.BetaEndNode;

import java.util.Collection;
import java.util.LinkedList;

public class RuleMemoryInsertTask extends Completer {
    private final Collection<RuntimeRule> rules;
    private final FireContext ctx;

    public RuleMemoryInsertTask(FireContext ctx, Collection<RuntimeRule> rules) {
        this.ctx = ctx;
        this.rules = rules;
    }

    @Override
    protected void execute() {
        Collection<BetaEndNode> changedBetaEndNodes = new LinkedList<>();
        for (RuntimeRule rule : rules) {
            BetaEndNode[] ruleBetaEndNodes = rule.getAllBetaEndNodes();
            for (BetaEndNode endNode : ruleBetaEndNodes) {
                if (endNode.isInsertAvailable()) {
                    changedBetaEndNodes.add(endNode);
                }
            }
        }

        tailCall(
                changedBetaEndNodes,
                n -> new NodeDeltaTask(RuleMemoryInsertTask.this, n, ctx)
        );
    }
}

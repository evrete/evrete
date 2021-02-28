package org.evrete.runtime.async;

import org.evrete.runtime.BetaEndNode;

import java.util.Collection;

public class RuleMemoryInsertTask extends Completer {
    private static final long serialVersionUID = 7911593735990639599L;
    private final Collection<BetaEndNode> deltaEndNodes;
    private final boolean deltaOnly;


    public RuleMemoryInsertTask(Collection<BetaEndNode> deltaEndNodes, boolean deltaOnly) {
        this.deltaEndNodes = deltaEndNodes;
        this.deltaOnly = deltaOnly;
    }

    @Override
    protected void execute() {
        tailCall(
                deltaEndNodes,
                n -> new NodeDeltaTask(RuleMemoryInsertTask.this, n, deltaOnly)
        );
    }
}

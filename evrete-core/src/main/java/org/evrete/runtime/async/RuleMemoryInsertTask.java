package org.evrete.runtime.async;

import org.evrete.runtime.BetaEndNode;
import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.Mask;

import java.util.Collection;

public class RuleMemoryInsertTask extends Completer {
    private static final long serialVersionUID = 7911593735990639599L;
    private final Collection<BetaEndNode> deltaEndNodes;
    private final boolean deltaOnly;
    private final transient Mask<MemoryAddress> matchMask;


    public RuleMemoryInsertTask(Collection<BetaEndNode> deltaEndNodes, Mask<MemoryAddress> matchMask, boolean deltaOnly) {
        this.deltaEndNodes = deltaEndNodes;
        this.deltaOnly = deltaOnly;
        this.matchMask = matchMask;
    }

    @Override
    protected void execute() {
        tailCall(
                deltaEndNodes,
                n -> new NodeDeltaTask(RuleMemoryInsertTask.this, matchMask, n, deltaOnly)
        );
    }
}

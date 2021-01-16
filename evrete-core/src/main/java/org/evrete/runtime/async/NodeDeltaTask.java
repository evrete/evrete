package org.evrete.runtime.async;

import org.evrete.runtime.BetaConditionNode;
import org.evrete.runtime.BetaMemoryNode;

import java.util.concurrent.CountedCompleter;


//TODO input mask
public class NodeDeltaTask extends Completer {
    private final BetaConditionNode node;
    private final BetaConditionNode[] sources;
    private final boolean deltaOnly;

    public NodeDeltaTask(Completer completer, BetaConditionNode node, boolean deltaOnly) {
        super(completer);
        this.node = node;
        this.sources = node.getConditionSources();
        this.deltaOnly = deltaOnly;
    }

    private NodeDeltaTask(NodeDeltaTask parent, BetaConditionNode node) {
        this(parent, node, parent.deltaOnly);
    }

    @Override
    protected void execute() {
        // Compute deltas of parent nodes
        tailCall(
                sources,
                cn -> new NodeDeltaTask(NodeDeltaTask.this, cn)
        );
    }

    @Override
    // Compute this node's delta
    public void onCompletion(CountedCompleter<?> caller) {
        node.computeDelta(deltaOnly);

        // Once this node's delta is computed, it's safe
        // to merge parent nodes' deltas
        for (BetaMemoryNode<?> node : sources) {
            node.mergeDelta();
        }
    }
}

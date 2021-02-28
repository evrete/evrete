package org.evrete.runtime.async;

import org.evrete.runtime.BetaConditionNode;

import java.util.concurrent.CountedCompleter;


//TODO input mask
public class NodeDeltaTask extends Completer {
    private static final long serialVersionUID = -9061292058914410992L;
    private final transient BetaConditionNode node;
    private final transient BetaConditionNode[] sources;
    private final boolean deltaOnly;

    NodeDeltaTask(Completer completer, BetaConditionNode node, boolean deltaOnly) {
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
    }
}

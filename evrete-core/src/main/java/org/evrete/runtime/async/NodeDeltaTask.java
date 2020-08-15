package org.evrete.runtime.async;

import org.evrete.runtime.BetaEvaluationContext;
import org.evrete.runtime.FireContext;
import org.evrete.runtime.memory.BetaConditionNode;
import org.evrete.runtime.memory.BetaEndNode;
import org.evrete.runtime.memory.BetaMemoryNode;

import java.util.concurrent.CountedCompleter;


//TODO input mask
public class NodeDeltaTask extends Completer {
    protected final BetaConditionNode node;
    private final BetaEvaluationContext ctx;
    private final BetaConditionNode[] sources;

    private NodeDeltaTask(Completer completer, BetaConditionNode node, BetaEvaluationContext ctx) {
        super(completer);
        this.ctx = ctx;
        this.node = node;
        this.sources = node.getConditionSources();
    }

    public NodeDeltaTask(Completer parent, BetaEndNode endNode, FireContext ctx) {
        this(parent, endNode, new BetaEvaluationContext(ctx));
    }

    private NodeDeltaTask(NodeDeltaTask parent, BetaConditionNode node) {
        this(parent, node, parent.ctx);
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
        node.computeDelta(ctx);

        // Once this node's delta is computed, it's safe
        // to merge parent nodes' deltas
        for (BetaMemoryNode<?> node : sources) {
            node.mergeDelta();
        }
    }
}

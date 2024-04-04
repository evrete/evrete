package org.evrete.runtime.async;

import org.evrete.runtime.BetaConditionNode;
import org.evrete.runtime.Mask;
import org.evrete.runtime.MemoryAddress;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class NodeDeltaTask extends Completer {
    private static final long serialVersionUID = -9061292058914410992L;
    private final transient BetaConditionNode node;
    private final transient Collection<BetaConditionNode> sources;
    private final boolean deltaOnly;
    private transient final Mask<MemoryAddress> matchMask;

    NodeDeltaTask(Completer completer, Mask<MemoryAddress> matchMask, BetaConditionNode node, boolean deltaOnly) {
        super(completer);
        this.node = node;
        if (matchMask != null) {
            this.sources = Arrays.stream(node.getConditionSources())
                    .filter(n -> matchMask.intersects(n.getDescriptor().getMemoryMask()))
                    .collect(Collectors.toList());
        } else {
            this.sources = Arrays.asList(node.getConditionSources());
        }
        this.deltaOnly = deltaOnly;
        this.matchMask = matchMask;
    }

    private NodeDeltaTask(NodeDeltaTask parent, BetaConditionNode node) {
        this(parent, parent.matchMask, node, parent.deltaOnly);
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
    protected void onCompletion() {
        node.computeDelta(deltaOnly);
    }
}

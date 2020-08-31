package org.evrete.runtime.async;

import org.evrete.api.KeysStore;
import org.evrete.runtime.FactType;
import org.evrete.runtime.RuntimeRuleImpl;
import org.evrete.runtime.memory.BetaConditionNode;
import org.evrete.runtime.memory.BetaMemoryNode;
import org.evrete.util.Bits;

public class NodeCleanTask extends Completer {
    private final BetaConditionNode node;
    private final RuntimeRuleImpl rule;
    private final FactType[][] grouping;
    private final KeysStore subject;
    private final Bits deleteMask;

    public NodeCleanTask(Completer completer, BetaConditionNode node, RuntimeRuleImpl rule, Bits deleteMask) {
        super(completer);
        this.node = node;
        this.rule = rule;
        this.grouping = node.getGrouping();
        this.subject = node.getMainStore();
        this.deleteMask = deleteMask;
    }

    private NodeCleanTask(NodeCleanTask parent, BetaConditionNode node) {
        this(parent, node, parent.rule, parent.deleteMask);
    }

    @Override
    protected void execute() {
        for (BetaMemoryNode<?> source : node.getSources()) {
            Bits sourceMask = source.getTypeMask();
            if (source.isConditionNode() && sourceMask.intersects(deleteMask)) {
                BetaConditionNode sourceNode = (BetaConditionNode) source;
                if (!sourceNode.getMainStore().isEmpty()) {
                    forkNew(new NodeCleanTask(this, sourceNode));
                }
            }
        }

        // Local execution
        ValueRowPredicate[] predicates = ValueRowPredicate.predicates(grouping, rule.getDeletedKeys());// new ValueRowPredicate[grouping.length];
        subject.delete(predicates);
    }


}

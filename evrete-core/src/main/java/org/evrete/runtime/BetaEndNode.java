package org.evrete.runtime;

import org.evrete.api.Type;

//TODO !!! optimize by sharing the value array and pre-built functions
public class BetaEndNode extends BetaConditionNode {
    private final FactType[] entryNodes;

    BetaEndNode(RuntimeRuleImpl rule, ConditionNodeDescriptor nodeDescriptor) {
        super(rule, nodeDescriptor, create(nodeDescriptor.getSources(), rule));
        FactType[] factTypes = nodeDescriptor.getEvalGrouping()[0];
        assert factTypes.length == nodeDescriptor.getTypes().length;
        this.entryNodes = nodeDescriptor.getEvalGrouping()[0];
    }

    private static BetaMemoryNode<?>[] create(NodeDescriptor[] sources, RuntimeRuleImpl rule) {
        BetaMemoryNode<?>[] result = new BetaMemoryNode<?>[sources.length];
        for (int i = 0; i < sources.length; i++) {
            result[i] = create(rule, sources[i]);
        }
        return result;
    }

    private static BetaMemoryNode<?> create(RuntimeRuleImpl rule, NodeDescriptor desc) {
        if (desc.isConditionNode()) {
            return new BetaConditionNode(
                    rule, (ConditionNodeDescriptor) desc,
                    create(desc.getSources(), rule)
            );
        } else {
            EntryNodeDescriptor descriptor = (EntryNodeDescriptor) desc;
            return new BetaEntryNode(descriptor, rule.resolve(descriptor.getFactType()));
        }
    }

    private static void cmd(BetaConditionNode node) {
        node.commitDelta1();
        for (BetaMemoryNode<?> source : node.getSources()) {
            if (source instanceof BetaConditionNode) {
                cmd((BetaConditionNode) source);
            }
        }
    }

    // TODO !!!!! optimize, create a set of involved types
    boolean dependsOn(Type<?> type) {
        for (FactType entry : entryNodes) {
            if (entry.getType().equals(type)) {
                return true;
            }
        }
        return false;
    }

    @Override
    //TODO !!!! bad interfaces/abstract methods
    public void commitDelta() {
        cmd(this);
    }
}

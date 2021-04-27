package org.evrete.runtime;

import org.evrete.api.KeyMode;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;
import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.Mask;

public class BetaEndNode extends BetaConditionNode implements RhsFactGroup {
    private final RuntimeFactType[] entryNodes;
    private final Mask<MemoryAddress> memoryMask;

    BetaEndNode(RuntimeRuleImpl rule, ConditionNodeDescriptor nodeDescriptor, boolean singleGroup) {
        super(rule, nodeDescriptor, create(nodeDescriptor.getSources(), rule));
        this.entryNodes = rule.asRuntimeTypes(nodeDescriptor.getTypes());
        this.memoryMask = nodeDescriptor.getMemoryMask();
        setMergeToMain(!singleGroup);
    }

    private static BetaMemoryNode create(RuntimeRuleImpl rule, NodeDescriptor desc) {
        if (desc.isConditionNode()) {
            return new BetaConditionNode(
                    rule, (ConditionNodeDescriptor) desc,
                    create(desc.getSources(), rule)
            );
        } else {
            EntryNodeDescriptor descriptor = (EntryNodeDescriptor) desc;
            return new BetaEntryNode(rule.getRuntime(), descriptor);
        }
    }

    private static BetaMemoryNode[] create(NodeDescriptor[] sources, RuntimeRuleImpl rule) {
        BetaMemoryNode[] result = new BetaMemoryNode[sources.length];
        for (int i = 0; i < sources.length; i++) {
            result[i] = create(rule, sources[i]);
        }
        return result;
    }

    @Override
    public ReIterator<MemoryKey> keyIterator(KeyMode mode) {
        return iterator(mode);
    }

    @Override
    public RuntimeFactType[] types() {
        return entryNodes;
    }

    @Override
    //TODO !!!! bad interfaces/abstract methods
    public void commitDelta() {
        forEachConditionNode(AbstractBetaConditionNode::commitDelta1);
    }

    @Override
    public Mask<MemoryAddress> getMemoryMask() {
        return memoryMask;
    }
}

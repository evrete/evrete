package org.evrete.runtime.memory;

import org.evrete.runtime.RuntimeFactType;
import org.evrete.runtime.RuntimeRule;
import org.evrete.runtime.structure.ConditionNodeDescriptor;
import org.evrete.runtime.structure.EntryNodeDescriptor;
import org.evrete.runtime.structure.FactType;
import org.evrete.runtime.structure.NodeDescriptor;

public class BetaEndNode extends BetaConditionNode {
    public static final BetaEndNode[] ZERO_ARRAY = new BetaEndNode[0];

    public BetaEndNode(RuntimeRule rule, ConditionNodeDescriptor nodeDescriptor) {
        super(rule, nodeDescriptor, create(nodeDescriptor.getSources(), rule));
        FactType[] factTypes = nodeDescriptor.getEvalGrouping()[0];
        assert factTypes.length == nodeDescriptor.getTypes().length;
    }

    private static BetaMemoryNode<?>[] create(NodeDescriptor[] sources, RuntimeRule rule) {
        BetaMemoryNode<?>[] result = new BetaMemoryNode<?>[sources.length];
        for (int i = 0; i < sources.length; i++) {
            result[i] = create(rule, sources[i]);
        }
        return result;
    }

    //TODO !!! optimize
    public boolean isInsertAvailable() {
        for (RuntimeFactType entryNode : getEntryNodes()) {
            if (entryNode.isInsertDeltaAvailable()) {
                return true;
            }
        }
        return false;
    }

    private static BetaMemoryNode<?> create(RuntimeRule rule, NodeDescriptor desc) {
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

    public RuntimeFactType[] getEntryNodes() {
        return getGrouping()[0];
    }
}

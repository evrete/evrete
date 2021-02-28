package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.collections.JoinReIterator;

//TODO !!! optimize by sharing the value array and pre-built functions
public class BetaEndNode extends BetaConditionNode implements RhsFactGroup {
    private final FactType[] entryNodes;

    BetaEndNode(RuntimeRuleImpl rule, ConditionNodeDescriptor nodeDescriptor, boolean singleGroup) {
        super(rule, nodeDescriptor, create(nodeDescriptor.getSources(), rule));
        FactType[] factTypes = nodeDescriptor.getEvalGrouping()[0];
        assert factTypes.length == nodeDescriptor.getTypes().length;
        this.entryNodes = nodeDescriptor.getEvalGrouping()[0];
        setMergeToMain(!singleGroup);
    }

    private static BetaMemoryNode<?> create(RuntimeRuleImpl rule, NodeDescriptor desc) {
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

    @Override
    public ReIterator<ValueRow[]> keyIterator(boolean delta) {
        return delta ?
                JoinReIterator.of(iterator(KeyMode.UNKNOWN_UNKNOWN), iterator(KeyMode.KNOWN_UNKNOWN))
                :
                iterator(KeyMode.MAIN);
    }

    @Override
    public ReIterator<FactHandleVersioned> factIterator(FactType type, ValueRow row) {
        KeyMode mode = KeyMode.values()[row.getTransient()];
        SessionMemory memory = getRuntime().memory;
        return memory.get(type.getType()).get(type.getFields()).get(type.getAlphaMask()).iterator(mode, row);
    }

    private static BetaMemoryNode<?>[] create(NodeDescriptor[] sources, RuntimeRuleImpl rule) {
        BetaMemoryNode<?>[] result = new BetaMemoryNode<?>[sources.length];
        for (int i = 0; i < sources.length; i++) {
            result[i] = create(rule, sources[i]);
        }
        return result;
    }

    @Override
    public FactType[] types() {
        return entryNodes;
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

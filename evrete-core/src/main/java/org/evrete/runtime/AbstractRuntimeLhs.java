package org.evrete.runtime;

import java.util.ArrayList;
import java.util.Collection;


public abstract class AbstractRuntimeLhs {
    private final Collection<BetaEndNode> endNodes = new ArrayList<>();
    private final AbstractLhsDescriptor descriptor;
    private final RhsFactGroup[] rhsFactGroups;

    AbstractRuntimeLhs(RuntimeRuleImpl rule, AbstractLhsDescriptor descriptor) {
        this.descriptor = descriptor;
        // Init end nodes first
        RhsFactGroupDescriptor[] allFactGroups = descriptor.getAllFactGroups();
        this.rhsFactGroups = new RhsFactGroup[allFactGroups.length];

        for (RhsFactGroupDescriptor groupDescriptor : allFactGroups) {
            int groupIndex = groupDescriptor.getFactGroupIndex();
            RhsFactGroup factGroup;
            if (groupDescriptor.isLooseGroup()) {
                factGroup = new RhsFactGroupAlpha(rule.getRuntime().getMemory(), groupDescriptor);
            } else {
                ConditionNodeDescriptor finalNode = groupDescriptor.getFinalNode();
                assert finalNode != null;
                boolean singleOutNode = allFactGroups.length == 1;
                BetaEndNode endNode = new BetaEndNode(rule, finalNode, singleOutNode);
                endNodes.add(endNode);
                factGroup = endNode;
            }
            rhsFactGroups[groupIndex] = factGroup;
        }
    }

    RhsFactGroup[] getFactGroups() {
        return this.rhsFactGroups;
    }

    Collection<BetaEndNode> getEndNodes() {
        return endNodes;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "descriptor=" + descriptor +
                '}';
    }
}

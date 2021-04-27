package org.evrete.runtime;

import java.util.ArrayList;
import java.util.Collection;

class RuntimeLhs {
    private final Collection<BetaEndNode> endNodes = new ArrayList<>();
    private final AbstractLhsDescriptor descriptor;
    private final RhsFactGroup[] rhsFactGroups;

    RuntimeLhs(RuntimeRuleImpl rule, LhsDescriptor descriptor) {
        this.descriptor = descriptor;
        // Init end nodes first
        RhsFactGroupDescriptor[] allFactGroups = descriptor.getAllFactGroups();
        this.rhsFactGroups = new RhsFactGroup[allFactGroups.length];

        int groupIndex = 0;
        for (RhsFactGroupDescriptor groupDescriptor : allFactGroups) {
            //int groupIndex = groupDescriptor.getFactGroupIndex();
            RhsFactGroup factGroup;
            if (groupDescriptor.isLooseGroup()) {
                factGroup = new RhsFactGroupAlpha(rule, groupDescriptor);
            } else {
                ConditionNodeDescriptor finalNode = groupDescriptor.getFinalNode();
                assert finalNode != null;
                boolean singleOutNode = allFactGroups.length == 1;
                BetaEndNode endNode = new BetaEndNode(rule, finalNode, singleOutNode);
                endNodes.add(endNode);
                factGroup = endNode;
            }
            rhsFactGroups[groupIndex++] = factGroup;
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

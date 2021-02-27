package org.evrete.runtime;

import java.util.ArrayList;
import java.util.Collection;


public abstract class AbstractRuntimeLhs {
    //final FactIterationState[][] factState;
    private final Collection<BetaEndNode> endNodes = new ArrayList<>();
    private final AbstractLhsDescriptor descriptor;
    private final RhsFactGroup[] rhsFactGroups;

    private AbstractRuntimeLhs(RuntimeRuleImpl rule, AbstractRuntimeLhs parent, AbstractLhsDescriptor descriptor) {
        this.descriptor = descriptor;
        //this.parent = parent;
        assert parent == null; // No aggregate groups yet
        // Init end nodes first
        RhsFactGroupDescriptor[] allFactGroups = descriptor.getAllFactGroups();
        //private final AbstractRuntimeLhs parent;
        this.rhsFactGroups = new RhsFactGroup[allFactGroups.length];
        //ValueRow[][] keyState = new ValueRow[allFactGroups.length][];
        //this.factState = new FactIterationState[allFactGroups.length][];

        RhsFactGroupAlpha alpha = null;
        for (RhsFactGroupDescriptor groupDescriptor : allFactGroups) {
            int groupIndex = groupDescriptor.getFactGroupIndex();
            //this.factState[groupIndex] = new FactIterationState[groupDescriptor.getTypes().length];
/*
            for (int i = 0; i < this.factState[groupIndex].length; i++) {
                Type<?> factType = groupDescriptor.getTypes()[i].getType();
                this.factState[groupIndex][i] = new FactIterationState(rule.getRuntime().getMemory().get(factType));
            }
*/

            RhsFactGroup factGroup;
            if (groupDescriptor.isLooseGroup()) {
                factGroup = new RhsFactGroupAlpha(rule.getRuntime().getMemory(), groupDescriptor);
            } else {
                ConditionNodeDescriptor finalNode = groupDescriptor.getFinalNode();
                if (finalNode != null) {
                    BetaEndNode endNode = new BetaEndNode(rule, finalNode);
                    endNodes.add(endNode);
                    factGroup = new RhsFactGroupBeta(rule.getRuntime().getMemory(), groupDescriptor, endNode);
                } else {
                    //TODO !!!!! make it impossible for final node to be null
                    throw new UnsupportedOperationException();
                }
            }
            rhsFactGroups[groupIndex] = factGroup;
        }
    }

    AbstractRuntimeLhs(RuntimeRuleImpl rule, LhsDescriptor descriptor) {
        this(rule, null, descriptor);
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

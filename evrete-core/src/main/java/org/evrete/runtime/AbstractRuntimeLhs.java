package org.evrete.runtime;

import org.evrete.api.RuntimeFact;
import org.evrete.api.ValueRow;
import org.evrete.runtime.memory.BetaEndNode;
import org.evrete.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;


public abstract class AbstractRuntimeLhs {
    final RuntimeFact[][] factState;
    private final Collection<BetaEndNode> endNodes = new ArrayList<>();
    private final AbstractLhsDescriptor descriptor;
    private final AbstractRuntimeLhs parent;
    private final RhsFactGroup[] rhsFactGroups;
    private final RhsFactGroupAlpha alphaFactGroup;
    private final RhsFactGroupBeta[] betaFactGroups;
    private final ValueRow[][] keyState;

    AbstractRuntimeLhs(RuntimeRuleImpl rule, AbstractRuntimeLhs parent, AbstractLhsDescriptor descriptor) {
        this.descriptor = descriptor;
        this.parent = parent;

        // Init end nodes first
        RhsFactGroupDescriptor[] allFactGroups = descriptor.getAllFactGroups();
        this.rhsFactGroups = new RhsFactGroup[allFactGroups.length];
        this.keyState = new ValueRow[allFactGroups.length][];
        this.factState = new RuntimeFact[allFactGroups.length][];

        RhsFactGroupAlpha alpha = null;
        for (RhsFactGroupDescriptor groupDescriptor : allFactGroups) {
            int groupIndex = groupDescriptor.getFactGroupIndex();
            this.factState[groupIndex] = new RuntimeFact[groupDescriptor.getTypes().length];

            RhsFactGroup factGroup;
            if (groupDescriptor.isLooseGroup()) {
                factGroup = alpha = new RhsFactGroupAlpha(rule, groupDescriptor, factState);
            } else {
                ConditionNodeDescriptor finalNode = groupDescriptor.getFinalNode();
                if (finalNode != null) {
                    BetaEndNode endNode = new BetaEndNode(rule, finalNode);
                    endNodes.add(endNode);
                    factGroup = new RhsFactGroupBeta(groupDescriptor, endNode, keyState, factState);
                } else {
                    RuntimeFactTypeKeyed singleType = rule.resolve(groupDescriptor.getTypes()[0]);
                    factGroup = new RhsFactGroupBeta(groupDescriptor, singleType, keyState, factState);
                }
            }
            this.rhsFactGroups[groupIndex] = factGroup;
        }
        this.betaFactGroups = CollectionUtils.filter(RhsFactGroupBeta.class, rhsFactGroups, group -> !group.isAlpha());
        this.alphaFactGroup = alpha;
    }

    AbstractRuntimeLhs(RuntimeRuleImpl rule, LhsDescriptor descriptor) {
        this(rule, null, descriptor);
    }


    public RhsFactGroupAlpha getAlphaFactGroup() {
        return alphaFactGroup;
    }

    public RhsFactGroupBeta[] getBetaFactGroups() {
        return betaFactGroups;
    }

    public ValueRow[][] getKeyState() {
        return keyState;
    }

    @SuppressWarnings("unchecked")
    public <T extends RhsFactGroup> T getGroup(RhsFactGroupDescriptor descriptor) {
        return (T) rhsFactGroups[descriptor.getFactGroupIndex()];
    }

    public AbstractLhsDescriptor getDescriptor() {
        return descriptor;
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

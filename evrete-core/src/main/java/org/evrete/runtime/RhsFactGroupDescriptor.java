package org.evrete.runtime;

import java.util.Arrays;
import java.util.Collection;

class RhsFactGroupDescriptor {
    static final RhsFactGroupDescriptor[] ZERO_ARRAY = new RhsFactGroupDescriptor[0];
    private final FactType[] types;
    private final boolean looseGroup;
    private final ConditionNodeDescriptor finalNode;

    private RhsFactGroupDescriptor(ConditionNodeDescriptor finalNode, FactType[] types, boolean looseGroup) {
        this.types = types;
        this.looseGroup = looseGroup;
        this.finalNode = finalNode;
    }

    RhsFactGroupDescriptor(ConditionNodeDescriptor finalNode) {
        this(finalNode, finalNode.getTypes(), false);
    }

    RhsFactGroupDescriptor(Collection<FactType> looseTypes) {
        this(null, looseTypes.toArray(FactType.ZERO_ARRAY), true);
        for (FactType t : looseTypes) {
            if (t.getFields().size() > 0) {
                throw new IllegalStateException();
            }
        }
    }

    ConditionNodeDescriptor getFinalNode() {
        return finalNode;
    }

    public FactType[] getTypes() {
        return types;
    }

    boolean isLooseGroup() {
        return looseGroup;
    }

    @Override
    public String toString() {
        return "{" +
                "types=" + Arrays.toString(types) +
                '}';
    }
}

package org.evrete.runtime;

import org.evrete.api.Masked;
import org.evrete.util.Bits;

import java.util.Arrays;
import java.util.Collection;

public class RhsFactGroupDescriptor implements Masked {
    static final RhsFactGroupDescriptor[] ZERO_ARRAY = new RhsFactGroupDescriptor[0];
    private final FactType[] types;
    private final boolean looseGroup;
    private final ConditionNodeDescriptor finalNode;
    private final Bits mask = new Bits();

    private RhsFactGroupDescriptor(ConditionNodeDescriptor finalNode, FactType[] types, boolean looseGroup) {
        this.types = types;
        this.looseGroup = looseGroup;
        this.finalNode = finalNode;
    }

    RhsFactGroupDescriptor(ConditionNodeDescriptor finalNode) {
        this(finalNode, finalNode.getEvalGrouping()[0], false);
    }

    RhsFactGroupDescriptor(Collection<FactType> looseTypes) {
        this(null, looseTypes.toArray(FactType.ZERO_ARRAY), true);
        for (FactType t : looseTypes) {
            if (t.getFields().size() > 0) {
                throw new IllegalStateException();
            }
        }
    }

    @Override
    public Bits getMask() {
        return mask;
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

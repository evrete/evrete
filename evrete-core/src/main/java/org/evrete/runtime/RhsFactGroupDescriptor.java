package org.evrete.runtime;

import org.evrete.api.Masked;
import org.evrete.util.Bits;

import java.util.Arrays;
import java.util.Collection;

public class RhsFactGroupDescriptor implements Masked {
    static final RhsFactGroupDescriptor[] ZERO_ARRAY = new RhsFactGroupDescriptor[0];
    private final int factGroupIndex;
    private final int keyGroupIndex;
    private final FactType[] types;
    private final boolean looseGroup;
    private final ConditionNodeDescriptor finalNode;
    private final boolean allUniqueKeysAndAlpha;
    private final Bits mask = new Bits();
    private final AbstractLhsDescriptor lhsDescriptor;

    private RhsFactGroupDescriptor(AbstractLhsDescriptor lhsDescriptor, int factGroupIndex, int keyGroupIndex, ConditionNodeDescriptor finalNode, FactType[] types, boolean looseGroup) {
        this.lhsDescriptor = lhsDescriptor;
        this.factGroupIndex = factGroupIndex;
        this.keyGroupIndex = keyGroupIndex;
        this.types = types;
        this.looseGroup = looseGroup;
        this.finalNode = finalNode;
        boolean au = true;
        for (FactType t : types) {
            mask.or(t.getMask());
            if (!t.isUniqueKeyAndAlpha()) {
                au = false;
            }
            t.setFactGroup(this);
        }
        this.allUniqueKeysAndAlpha = au;
    }

    public RhsFactGroupDescriptor(AbstractLhsDescriptor lhsDescriptor, int factGroupIndex, int keyGroupIndex, ConditionNodeDescriptor finalNode) {
        this(lhsDescriptor, factGroupIndex, keyGroupIndex, finalNode, finalNode.getEvalGrouping()[0], false);
    }

    int positionOf(FactType type) {
        for (int pos = 0; pos < types.length; pos++) {
            if (types[pos] == type) {
                return pos;
            }
        }
        throw new IllegalStateException();
    }

    @Override
    public Bits getMask() {
        return mask;
    }

    public RhsFactGroupDescriptor(AbstractLhsDescriptor lhsDescriptor, int factGroupIndex, int keyGroupIndex, FactType keyedType) {
        this(lhsDescriptor, factGroupIndex, keyGroupIndex, null, new FactType[]{keyedType}, false);
        if (keyedType.getFields().size() == 0) {
            throw new IllegalStateException();
        }
    }

    public RhsFactGroupDescriptor(AbstractLhsDescriptor lhsDescriptor, int factGroupIndex, Collection<FactType> looseTypes) {
        this(lhsDescriptor, factGroupIndex, -1, null, looseTypes.toArray(FactType.ZERO_ARRAY), true);
        for (FactType t : looseTypes) {
            if (t.getFields().size() > 0) {
                throw new IllegalStateException();
            }
        }
    }

    public AbstractLhsDescriptor getLhsDescriptor() {
        return lhsDescriptor;
    }

    public ConditionNodeDescriptor getFinalNode() {
        return finalNode;
    }

    public int getFactGroupIndex() {
        return factGroupIndex;
    }


    public int getKeyGroupIndex() {
        return keyGroupIndex;
    }

    public FactType[] getTypes() {
        return types;
    }

    public boolean isLooseGroup() {
        return looseGroup;
    }

    public boolean isAllUniqueKeysAndAlpha() {
        return this.allUniqueKeysAndAlpha;
    }

    @Override
    public String toString() {
        return "{" +
                "types=" + Arrays.toString(types) +
                '}';
    }
}

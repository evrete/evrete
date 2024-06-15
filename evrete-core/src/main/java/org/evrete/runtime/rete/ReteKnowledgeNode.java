package org.evrete.runtime.rete;

import org.evrete.runtime.FactType;
import org.evrete.runtime.GroupedFactType;
import org.evrete.runtime.Mask;

public abstract class ReteKnowledgeNode extends ReteNode<ReteKnowledgeNode> {
    public static ReteKnowledgeNode[] EMPTY_ARRAY = new ReteKnowledgeNode[0];
    private final Mask<FactType> factTypeMask;
    private final Mask<GroupedFactType> groupedFactTypeMask;
    final int[] inGroupIndices;

    protected ReteKnowledgeNode(GroupedFactType factType) {
        super(EMPTY_ARRAY);
        this.factTypeMask = Mask.factTypeMask().set(factType);
        this.groupedFactTypeMask = Mask.inGroupMask().set(factType);
        this.inGroupIndices = new int[]{factType.getInGroupIndex()};
    }

    protected ReteKnowledgeNode(ReteKnowledgeNode[] sourceNodes) {
        super(sourceNodes);
        Mask<FactType> fm = Mask.factTypeMask();
        Mask<GroupedFactType> gm = Mask.inGroupMask();
        for (ReteKnowledgeNode sourceNode : sourceNodes) {
            fm.or(sourceNode.factTypeMask);
            gm.or(sourceNode.groupedFactTypeMask);
        }

        this.factTypeMask = fm;
        this.groupedFactTypeMask = gm;
        this.inGroupIndices = gm.getSetBits();
    }

    public final Mask<FactType> getFactTypeMask() {
        return this.factTypeMask;
    }
}

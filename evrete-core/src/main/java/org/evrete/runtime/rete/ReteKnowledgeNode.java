package org.evrete.runtime.rete;

import org.evrete.runtime.FactType;
import org.evrete.runtime.Mask;

import java.util.ArrayList;
import java.util.List;

public abstract class ReteKnowledgeNode extends ReteNode<ReteKnowledgeNode> {
    public static ReteKnowledgeNode[] EMPTY_ARRAY = new ReteKnowledgeNode[0];
    private final Mask<FactType> factTypeMask;
    //private final Mask<GroupedFactType> groupedFactTypeMask;
    /**
     * This variable contains array of fact types served by this node.
     * It will define storage format for RETE condition nodes.
     */
    private final FactType[] nodeFactTypes;

    /**
     * Keeps reverse index mapping. In fact, it's a
     * <code>function(source node index, position of a fact inside that source)</code> that returns index of a
     * fact inside the {@link #nodeFactTypes} array
     */
    private final int[][] nodeFactTypesMapping;

    protected ReteKnowledgeNode(FactType factType) {
        super(EMPTY_ARRAY);
        this.factTypeMask = Mask.factTypeMask().set(factType);
        //this.groupedFactTypeMask = Mask.inGroupMask().set(factType);
        this.nodeFactTypes = new FactType[]{factType};
        this.nodeFactTypesMapping = new int[0][];
    }

    protected ReteKnowledgeNode(ReteKnowledgeNode[] sourceNodes) {
        super(sourceNodes);
        Mask<FactType> fm = Mask.factTypeMask();
        //Mask<GroupedFactType> gm = Mask.inGroupMask();
        List<FactType> groupedFactTypes = new ArrayList<>();
        this.nodeFactTypesMapping = new int[sourceNodes.length][];

        int nodeFactTypesIndex = 0;

        for (int sourceIndex = 0; sourceIndex < sourceNodes.length; sourceIndex++) {
            ReteKnowledgeNode sourceNode = sourceNodes[sourceIndex];
            FactType[] sourceNodeFactTypes = sourceNode.getNodeFactTypes();
            this.nodeFactTypesMapping[sourceIndex] = new int[sourceNodeFactTypes.length];
            for (int j = 0; j < sourceNodeFactTypes.length; j++) {
              FactType sourceFactType = sourceNodeFactTypes[j];
              groupedFactTypes.add(sourceFactType);
              this.nodeFactTypesMapping[sourceIndex][j] = nodeFactTypesIndex++;
            }

            fm.or(sourceNode.factTypeMask);
            //gm.or(sourceNode.groupedFactTypeMask);
        }


        this.factTypeMask = fm;
        //this.groupedFactTypeMask = gm;
        this.nodeFactTypes = groupedFactTypes.toArray(FactType.EMPTY_ARRAY);
    }

    public int[][] getNodeFactTypesMapping() {
        return nodeFactTypesMapping;
    }

    public FactType[] getNodeFactTypes() {
        return nodeFactTypes;
    }

    public final Mask<FactType> getFactTypeMask() {
        return this.factTypeMask;
    }
}

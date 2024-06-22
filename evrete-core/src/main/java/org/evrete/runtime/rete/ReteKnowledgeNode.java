package org.evrete.runtime.rete;

import org.evrete.runtime.ActiveType;
import org.evrete.runtime.FactType;
import org.evrete.runtime.MapOfList;
import org.evrete.runtime.Mask;

import java.util.ArrayList;
import java.util.List;

public abstract class ReteKnowledgeNode extends ReteNode<ReteKnowledgeNode> {
    public static ReteKnowledgeNode[] EMPTY_ARRAY = new ReteKnowledgeNode[0];
    /**
     * This variable contains array of fact types served by this node.
     * It will define storage format for RETE condition nodes.
     */
    private final FactType[] nodeFactTypes;

    private final MapOfList<ActiveType.Idx, Integer> typeToIndices;
    /**
     * Keeps reverse index mapping. In fact, it's a
     * <code>function(source node index, position of a fact inside that source)</code> that returns index of a
     * fact inside the {@link #nodeFactTypes} array
     */
    private final int[][] nodeFactTypesMapping;

    protected ReteKnowledgeNode(FactType factType) {
        super(EMPTY_ARRAY);
        this.nodeFactTypes = new FactType[]{factType};
        this.nodeFactTypesMapping = new int[0][];
        this.typeToIndices = new MapOfList<>();
        this.typeToIndices.add(factType.typeId(), 0);
    }

    protected ReteKnowledgeNode(ReteKnowledgeNode[] sourceNodes) {
        super(sourceNodes);
        List<FactType> nodeFactTypes = new ArrayList<>();
        this.nodeFactTypesMapping = new int[sourceNodes.length][];

        int nodeFactTypesIndex = 0;
        this.typeToIndices = new MapOfList<>();

        for (int sourceIndex = 0; sourceIndex < sourceNodes.length; sourceIndex++) {
            ReteKnowledgeNode sourceNode = sourceNodes[sourceIndex];
            FactType[] sourceNodeFactTypes = sourceNode.getNodeFactTypes();
            this.nodeFactTypesMapping[sourceIndex] = new int[sourceNodeFactTypes.length];
            for (int j = 0; j < sourceNodeFactTypes.length; j++) {
              FactType sourceFactType = sourceNodeFactTypes[j];
              nodeFactTypes.add(sourceFactType);
              this.nodeFactTypesMapping[sourceIndex][j] = nodeFactTypesIndex;
              this.typeToIndices.add(sourceFactType.typeId(), nodeFactTypesIndex);
              nodeFactTypesIndex++;
            }
        }
        this.nodeFactTypes = nodeFactTypes.toArray(FactType.EMPTY_ARRAY);
    }

    public MapOfList<ActiveType.Idx, Integer> getTypeToIndices() {
        return typeToIndices;
    }

    public int[][] getNodeFactTypesMapping() {
        return nodeFactTypesMapping;
    }

    public FactType[] getNodeFactTypes() {
        return nodeFactTypes;
    }
}

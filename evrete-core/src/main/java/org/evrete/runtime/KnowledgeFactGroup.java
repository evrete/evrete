package org.evrete.runtime;

import org.evrete.api.annotations.Nullable;
import org.evrete.runtime.rete.ReteGraph;
import org.evrete.runtime.rete.ReteKnowledgeConditionNode;
import org.evrete.runtime.rete.ReteKnowledgeEntryNode;
import org.evrete.runtime.rete.ReteKnowledgeNode;
import org.evrete.util.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Base class for fact groups. The engine uses this class to group mutually
 * independent fact declarations. If the system finds that a rule's beta conditions form
 * several non-intersecting graphs, each with its own terminal node, the fact types of those
 * graphs will be placed in different {@link Beta} groups. Fact declarations that do not
 * engage in any beta conditions will form a {@link Plain} group.
 * </p>
 * <p>
 * Fact grouping is the final stage in analyzing a rule's LHS (Left-Hand Side) and is a
 * relatively CPU-intensive operation. Once computed on the knowledge level, they will be used to
 * create the actual RETE condition beta-memories for each session-level rule.
 * </p>
 * <p>
 * Given the fact group definition, each rule may contain:
 * </p>
 * <ul>
 * <li>
 * Zero or more {@link Beta} groups
 * </li>
 * <li>
 * Zero or one {@link Plain} group
 * </li>
 * </ul>
 */
public abstract class KnowledgeFactGroup {
    public static final KnowledgeFactGroup[] EMPTY_ARRAY = new KnowledgeFactGroup[0];

    private final GroupedFactType[] entryNodes;
    //private final Mask<AlphaMemoryAddress> alphaMemoryMask;
    private final Mask<ActiveType> typeMask;
    // In a fact group, several fact declarations can be of the same type
    private final Map<ActiveType.Idx, int[]> typeToInGroupIndices;
    // Set of alpha locations in use
    private final Mask<AlphaAddress> alphaAddressMask;

    public KnowledgeFactGroup(GroupedFactType[] entryNodes) {
        this.entryNodes = entryNodes;
        this.typeMask = Mask.typeMask();
        this.alphaAddressMask = Mask.alphaAddressMask();


        MapOfSet<ActiveType.Idx, Integer> tempMap = new MapOfSet<>();
        for (GroupedFactType entryNode : entryNodes) {
            ActiveType type = entryNode.type();
            this.typeMask.set(type);
            this.alphaAddressMask.set(entryNode.getAlphaAddress());
            tempMap.add(type.getId(), entryNode.getInGroupIndex());
        }

        this.typeToInGroupIndices = new HashMap<>();
        tempMap.forEach(
                (type, groupIndices) -> {
                    this.typeToInGroupIndices.put(type, CommonUtils.toPrimitives(groupIndices));
                }
        );
    }

    public Mask<AlphaAddress> getAlphaAddressMask() {
        return alphaAddressMask;
    }

    @Nullable
    public int[] inGroupIndicesOfType(ActiveType.Idx activeTypeId) {
        return typeToInGroupIndices.get(activeTypeId);
    }

    public KnowledgeFactGroup(KnowledgeFactGroup other) {
        this.entryNodes = other.getEntryNodes();
        this.typeMask = other.typeMask;
        this.alphaAddressMask = other.alphaAddressMask;
        this.typeToInGroupIndices = other.typeToInGroupIndices;
    }

    public Mask<ActiveType> getTypeMask() {
        return typeMask;
    }

    public GroupedFactType[] getEntryNodes() {
        return entryNodes;
    }

    /**
     * @return new group descriptor
     * @see Plain
     */
    public static KnowledgeFactGroup fromPlainFactTypes(GroupedFactType[] factTypes) {
        return new Plain(factTypes);
    }

    /**
     * @return new group descriptor
     * @see Beta
     */
    public static KnowledgeFactGroup fromTerminalCondition(GroupedFactType[] factTypes, ReteKnowledgeConditionNode terminalNode) {
        ReteGraph<ReteKnowledgeNode, ReteKnowledgeEntryNode, ReteKnowledgeConditionNode> graph = ReteGraph.fromTerminalNode(terminalNode);
        return new Beta(factTypes, graph);
    }

    /**
     * Represents a group of fact types that do not participate in any beta-conditions
     * (conditions that involve joining multiple fact types).
     */
    static class Plain extends KnowledgeFactGroup {
        Plain(GroupedFactType[] factTypes) {
            super(factTypes);
        }
    }

    /**
     * Represents a fact group composed of LHS type declarations interconnected by beta conditions.
     * Beta conditions form a {@link ReteGraph} with a single terminal condition node and as many entry nodes
     * as this group's number of fact declarations.
     */
    static class Beta extends KnowledgeFactGroup {
        private final ReteGraph<ReteKnowledgeNode, ReteKnowledgeEntryNode, ReteKnowledgeConditionNode> graph;

        Beta(GroupedFactType[] factTypes, ReteGraph<ReteKnowledgeNode, ReteKnowledgeEntryNode, ReteKnowledgeConditionNode> graph) {
            super(factTypes);
            this.graph = graph;
        }

        public ReteGraph<ReteKnowledgeNode, ReteKnowledgeEntryNode, ReteKnowledgeConditionNode> getGraph() {
            return graph;
        }
    }
}

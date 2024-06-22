package org.evrete.runtime;

import org.evrete.runtime.rete.ReteGraph;
import org.evrete.runtime.rete.ReteKnowledgeConditionNode;
import org.evrete.runtime.rete.ReteKnowledgeEntryNode;
import org.evrete.runtime.rete.ReteKnowledgeNode;

import java.util.Collection;
import java.util.Collections;

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

    private final FactType[] entryNodes;
    private final Mask<ActiveType> typeMask;
    // Set of alpha locations in use
    private final Mask<AlphaAddress> alphaAddressMask;
    private final MapOfList<ActiveType.Idx, Integer> typeToIndices;

    public KnowledgeFactGroup(FactType[] entryNodes) {
        this.entryNodes = entryNodes;
        this.typeMask = Mask.typeMask();
        this.alphaAddressMask = Mask.alphaAddressMask();

        this.typeToIndices = new MapOfList<>();
        for (int i = 0; i < entryNodes.length; i++) {
            FactType entryNode = entryNodes[i];
            ActiveType type = entryNode.type();
            this.typeMask.set(type);
            this.alphaAddressMask.set(entryNode.getAlphaAddress());
            this.typeToIndices.add(type.getId(), i);
        }
    }

    public Mask<AlphaAddress> getAlphaAddressMask() {
        return alphaAddressMask;
    }

    public Collection<Integer> nodeIndices(ActiveType.Idx type) {
        return typeToIndices.getOrDefault(type, Collections.emptyList());
    }

    public KnowledgeFactGroup(KnowledgeFactGroup other) {
        this.entryNodes = other.getEntryNodes();
        this.typeMask = other.typeMask;
        this.alphaAddressMask = other.alphaAddressMask;
        this.typeToIndices = other.typeToIndices;
    }

    public Mask<ActiveType> getTypeMask() {
        return typeMask;
    }

    public FactType[] getEntryNodes() {
        return entryNodes;
    }

    /**
     * @return new group descriptor
     * @see Plain
     */
    public static KnowledgeFactGroup fromPlainFactTypes(FactType[] factTypes) {
        return new Plain(factTypes);
    }

    /**
     * @return new group descriptor
     * @see Beta
     */
    public static KnowledgeFactGroup fromTerminalCondition(ReteKnowledgeConditionNode terminalNode) {
        ReteGraph<ReteKnowledgeNode, ReteKnowledgeEntryNode, ReteKnowledgeConditionNode> graph = ReteGraph.fromTerminalNode(terminalNode);
        return new Beta(graph);
    }

    /**
     * Represents a group of fact types that do not participate in any beta-conditions
     * (conditions that involve joining multiple fact types).
     */
    static class Plain extends KnowledgeFactGroup {
        Plain(FactType[] factTypes) {
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

        Beta(ReteGraph<ReteKnowledgeNode, ReteKnowledgeEntryNode, ReteKnowledgeConditionNode> graph) {
            super(graph.terminalNode().getNodeFactTypes());
            this.graph = graph;
        }

        public ReteGraph<ReteKnowledgeNode, ReteKnowledgeEntryNode, ReteKnowledgeConditionNode> getGraph() {
            return graph;
        }
    }
}
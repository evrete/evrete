package org.evrete.runtime.rete;

/**
 * This super class represents a node in a Rete condition graph. Each node can have source nodes,
 * which are the nodes from which it receives data.
 * <p>
 * The knowledge-level {@link ReteNode} subclasses just keep collection of source nodes
 * and contain references to the node's condition.
 * </p>
 * <p>
 * The runtime subclasses evaluate changes in the source nodes, and, if conditions are met,
 * local changes are computed. These computed changes are then used by downstream nodes in a recursive
 * manner until the graph's terminal node is reached. Runtime (session) nodes follow
 * the {@link org.evrete.api.ReteMemory} contract.
 * </p>
 *
 * @param <T> the type of the node
 * @see ReteGraph
 * @see org.evrete.runtime.KnowledgeFactGroup
 */
public class ReteNode<T extends ReteNode<T>> {
    protected final T[] sourceNodes;

    protected ReteNode(T[] sourceNodes) {
        this.sourceNodes = sourceNodes;
    }

    /**
     * Returns the source nodes of this node.
     *
     * @return a collection of source nodes
     */
    public final T[] sourceNodes() {
        return sourceNodes;
    }

    public final boolean isConditionNode() {
        return this.sourceNodes.length > 0;
    }

}


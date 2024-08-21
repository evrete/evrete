package org.evrete.runtime.rete;

import org.evrete.util.CommonUtils;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <p>
 * This class defines a basic graph needed to implement the Rete condition evaluation
 * algorithm. The Rete algorithm evaluates conditions by propagating changes through a
 * network of nodes. To compute changes in the graph's terminal node, the changes in the
 * node's source nodes must be computed, and so on.
 * </p>
 *
 * @param <B> the base type of all nodes in the graph
 * @param <E> the type of the entry nodes in the graph
 * @param <C> the type of the condition node in the graph
 * @see ReteNode
 */
public final class ReteGraph<B extends ReteNode<B>, E extends B, C extends B> {

    private final C terminalNode;

    private ReteGraph(C terminalNode) {
        if(terminalNode.sourceNodes().length < 1) {
            throw new IllegalArgumentException("The terminal node must have at least one source node");
        } else {
            this.terminalNode = terminalNode;
        }
    }

    /**
     * Returns the terminal condition node of the graph.
     *
     * @return the terminal node
     */
    public C terminalNode() {
        return terminalNode;
    }

    @SuppressWarnings("unchecked")
    public void forEachConditionNode(Consumer<C> action) {
        this.forEachNode(b -> {
            if(b.isConditionNode()) {
                action.accept((C) b);
            }
        });
    }

    public void forEachNode(Consumer<B> action) {
        this.forEachNode(this.terminalNode, action);
    }

    private void forEachNode(B parent, Consumer<B> action) {
        action.accept(parent);
        for(B sourceNode : parent.sourceNodes()) {
            this.forEachNode(sourceNode, action);
        }
    }

    /**
     * Transforms this graph into another graph with different type parameters
     *
     * @return a transformed graph
     * @param <B1> the base type of the nodes in the new graph
     * @param <E1> the type of the entry nodes in the new graph
     * @param <C1> the type of the condition nodes in the new graph
     */
    @SuppressWarnings("unchecked")
    public <B1 extends ReteNode<B1>, E1 extends B1, C1 extends B1> ReteGraph<B1, E1, C1> transform(Class<B1> nodeType, BiFunction<C, B1[], C1> conditionNodeMapper, Function<E, E1> entryNodeMapper) {
        C1 newTerminalNode = (C1) transformNode(nodeType, this.terminalNode, conditionNodeMapper, entryNodeMapper);
        return new ReteGraph<>(newTerminalNode);
    }

    @SuppressWarnings("unchecked")
    private <B1 extends ReteNode<B1>, E1 extends B1, C1 extends B1> B1 transformNode(Class<B1> nodeType, B node, BiFunction<C, B1[], C1> conditionNodeMapper, Function<E, E1> entryNodeMapper) {
        if (node.isConditionNode()) {
            // The node is a condition node
            C conditionNode = (C) node;
            B[] sources = conditionNode.sourceNodes();
            B1[] newSources = CommonUtils.array(nodeType, sources.length);
            for (int i = 0; i < sources.length; i++) {
                newSources[i] = transformNode(nodeType, sources[i], conditionNodeMapper, entryNodeMapper);
            }
            return conditionNodeMapper.apply(conditionNode, newSources);
        } else {
            // The node is an entry node
            E entryNode = (E) node;
            return entryNodeMapper.apply(entryNode);

        }
    }

    public static <N extends ReteNode<N>, E extends N, T extends N> ReteGraph<N,E,T> fromTerminalNode(T terminalNode) {
        return new ReteGraph<>(terminalNode);
    }
}


package org.evrete.runtime;

import org.evrete.runtime.evaluation.BetaEvaluator;
import org.evrete.runtime.rete.ReteKnowledgeConditionNode;
import org.evrete.runtime.rete.ReteKnowledgeEntryNode;
import org.evrete.runtime.rete.ReteKnowledgeNode;
import org.evrete.util.MapFunction;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class KnowledgeFactGroupBuilder {
    private static final Logger LOGGER = Logger.getLogger(KnowledgeFactGroupBuilder.class.getName());

    static KnowledgeFactGroup[] build(Collection<FactType> factTypes, Collection<BetaEvaluator> flattenedBetaConditions) {

        // 1. Sort the conditions first.
        //    This way, less complex conditions will be evaluated first and reduce amount of data trickling down
        //    to more computationally intensive condition nodes.
        List<BetaEvaluator> betaEvaluators = new ArrayList<>(flattenedBetaConditions);
        betaEvaluators.sort(BetaEvaluator::compare);
        LOGGER.fine(() -> "Building fact groups for fact types: " + FactType.toSimpleDebugString(factTypes) + " and beta conditions: " + flattenedBetaConditions);

        // As every beta condition has a unique combination (unordered) of the fact types it is dealing with,
        // we'll be using a fact type mask as a means to build the groups.

        List<FactTypeNode> unallocatedEntryNodes = new ArrayList<>(factTypes.size());
        for (FactType factType : factTypes) {
            unallocatedEntryNodes.add(new FactTypeNode(factType));
        }

        List<EvaluatorNode> unallocatedConditionNodes = new LinkedList<>();

        for (BetaEvaluator evaluator : betaEvaluators) {
            LOGGER.fine(() -> "1. Processing " + evaluator + "...");

            Mask<FactType> evaluatorMask = evaluator.getTypeMask();

            // Match (and remove) unallocated entry nodes that match this evaluator
            // (they'll become this evaluator source nodes)
            Collection<FactTypeNode> evaluatorEntryNodes = new LinkedList<>();
            Iterator<FactTypeNode> it1 = unallocatedEntryNodes.iterator();
            while (it1.hasNext()) {
                FactTypeNode factTypeNode = it1.next();
                if (factTypeNode.intersects(evaluatorMask)) {
                    evaluatorEntryNodes.add(factTypeNode);
                    it1.remove();
                }
            }
            LOGGER.fine(() -> "2. Matching entry nodes " + evaluatorEntryNodes);

            // Match (and remove) previously created condition nodes
            // (they'll become this evaluator source nodes, too)
            Collection<EvaluatorNode> evaluatorConditionNodes = new LinkedList<>();
            Iterator<EvaluatorNode> it2 = unallocatedConditionNodes.iterator();
            while (it2.hasNext()) {
                EvaluatorNode evaluatorNode = it2.next();
                if (evaluatorNode.intersects(evaluatorMask)) {
                    evaluatorConditionNodes.add(evaluatorNode);
                    it2.remove();
                }
            }
            LOGGER.fine(() -> "3. Matching condition nodes " + evaluatorConditionNodes);

            // The condition must have at least two sources,
            if (evaluatorConditionNodes.size() + evaluatorEntryNodes.size() < 1) {
                throw new IllegalStateException("Unable to allocate fact types: invalid sources");
            }

            // and the sources must not have common fact types
            Mask<FactType> m1 = maskOf(evaluatorConditionNodes);
            Mask<FactType> m2 = maskOf(evaluatorEntryNodes);
            if (m1.intersects(m2)) {
                throw new IllegalStateException("Unable to allocate fact types: intersecting sources");
            }

            // Now we update the unallocated data
            unallocatedConditionNodes.removeAll(evaluatorConditionNodes);
            unallocatedEntryNodes.removeAll(evaluatorEntryNodes);

            // Create a new node
            EvaluatorNode evaluatorNode = new EvaluatorNode(evaluator, evaluatorEntryNodes, evaluatorConditionNodes);

            // And add it back to the unallocated condition nodes
            unallocatedConditionNodes.add(evaluatorNode);
        }

        // At this point we're supposed to have \:
        //   1) possibly empty collection of unallocated fact types,
        //      they will form a single plain fact group (w/o beta conditions).
        //   2) possibly empty collection of unallocated nested condition nodes,
        //      prototypes of the so-called terminal RETE nodes

        return arrange(unallocatedEntryNodes, unallocatedConditionNodes);
    }

    private static KnowledgeFactGroup[] arrange(List<FactTypeNode> unallocatedEntryNodes, List<EvaluatorNode> unallocatedNodes) {
        List<KnowledgeFactGroup> resultList = new LinkedList<>();

        // 1. Deal with the unallocated fact types first (the optional plain fact group)
        List<FactType> unallocatedTypes = unallocatedEntryNodes.stream()
                .map(FactTypeNode::getFactType)
                .sorted(Comparator.comparingInt(FactType::getInRuleIndex))
                .collect(Collectors.toList());
        int unallocatedSize = unallocatedTypes.size();
        if (unallocatedSize > 0) {
            FactType[] groupedFactTypes = new FactType[unallocatedSize];
            for (int i = 0; i < unallocatedSize; i++) {
                FactType factType = unallocatedTypes.get(i);
                groupedFactTypes[i] = factType;
            }
            resultList.add(KnowledgeFactGroup.fromPlainFactTypes(groupedFactTypes));
        }

        // 2. Create RETE beta groups from terminal condition nodes
        for (EvaluatorNode evaluatorNode : unallocatedNodes) {
            resultList.add(betaGroup(evaluatorNode));
        }

        return resultList.toArray(KnowledgeFactGroup.EMPTY_ARRAY);

    }

    private static KnowledgeFactGroup betaGroup(EvaluatorNode terminalNode) {
        // 1. Enumerate all entry nodes first
        final List<FactType> entryNodes = new ArrayList<>();
        terminalNode.forEachEntryNodeSource(node -> entryNodes.add(node.factType));
        int totalEntryNodes = entryNodes.size();
        assert totalEntryNodes > 0; // A graph must have entry nodes
        entryNodes.sort(Comparator.comparingInt(FactType::getInRuleIndex));

        // 2. Map fact types to RETE entry nodes
        MapFunction<FactType, ReteKnowledgeEntryNode> entryNodeMapping = new MapFunction<>();
        for (int i = 0; i < totalEntryNodes; i++) {
            FactType nodeFactType = entryNodes.get(i);
            // Create RETE entry node and save it
            ReteKnowledgeEntryNode reteEntryNode = new ReteKnowledgeEntryNode(nodeFactType);
            entryNodeMapping.putNew(nodeFactType, reteEntryNode);
        }

        // 3. Build the graph
        ReteKnowledgeConditionNode reteTerminalNode = terminalNode.toReteNode(entryNodeMapping);
        return KnowledgeFactGroup.fromTerminalCondition(reteTerminalNode);
    }


    private static Mask<FactType> ofConditionNodeComponents(Collection<FactTypeNode> evaluatorEntryNodes, Collection<EvaluatorNode> evaluatorConditionNodes) {
        Mask<FactType> total = Mask.factTypeMask();
        for (FactTypeNode factTypeNode : evaluatorEntryNodes) {
            total.or(factTypeNode.factTypeMask);
        }
        for (EvaluatorNode evaluatorNode : evaluatorConditionNodes) {
            total.or(evaluatorNode.factTypeMask);
        }
        return total;
    }

//    private static Mask<FactType> conditionMask(BetaEvaluator evaluator) {
//        // We're taking the first reference because others may have a different order,
//        // but the same combination of fact types.
//        return evaluator.getTypeMask();
//    }

    private static Mask<FactType> maskOf(FactType factType) {
        return Mask.factTypeMask().set(factType);
    }

    private static Mask<FactType> maskOf(Collection<? extends Node> nodes) {
        Mask<FactType> mask = Mask.factTypeMask();
        for (Node node : nodes) {
            mask.or(node.factTypeMask);
        }
        return mask;
    }

    private static class Node {
        final Mask<FactType> factTypeMask;

        Node(Mask<FactType> factTypeMask) {
            this.factTypeMask = factTypeMask;
        }

        boolean intersects(Mask<FactType> other) {
            return factTypeMask.intersects(other);
        }
    }

    private static class FactTypeNode extends Node {
        private final FactType factType;

        public FactTypeNode(FactType factType) {
            super(maskOf(factType));
            this.factType = factType;
        }

        public FactType getFactType() {
            return factType;
        }

        @Override
        public String toString() {
            return "Node{" +
                    "factType='" + factType.getVarName() + "'" +
                    '}';
        }
    }

    private static class EvaluatorNode extends Node {
        final BetaEvaluator evaluator;
        final Collection<FactTypeNode> entryNodeSources;
        final Collection<EvaluatorNode> conditionNodeSources;

        EvaluatorNode(BetaEvaluator evaluator, Collection<FactTypeNode> entryNodeSources, Collection<EvaluatorNode> conditionNodeSources) {
            super(ofConditionNodeComponents(entryNodeSources, conditionNodeSources));
            this.evaluator = evaluator;
            this.conditionNodeSources = conditionNodeSources;
            this.entryNodeSources = entryNodeSources;
        }

        void forEachEntryNodeSource(Consumer<FactTypeNode> consumer) {
            this.entryNodeSources.forEach(consumer);
            for (EvaluatorNode evaluatorNode : this.conditionNodeSources) {
                evaluatorNode.forEachEntryNodeSource(consumer);
            }
        }

        ReteKnowledgeConditionNode toReteNode(MapFunction<FactType, ReteKnowledgeEntryNode> entryNodeMapping) {
            List<ReteKnowledgeNode> sourceNodes = new ArrayList<>();
            // 1. Add fact type nodes as sources
            for (FactTypeNode factTypeNode : entryNodeSources) {
                sourceNodes.add(entryNodeMapping.apply(factTypeNode.factType));
            }

            // 2. Recursively add condition sources
            for (EvaluatorNode conditionNode : conditionNodeSources) {
                sourceNodes.add(conditionNode.toReteNode(entryNodeMapping));
            }

            return new ReteKnowledgeConditionNode(this.evaluator, sourceNodes.toArray(ReteKnowledgeNode.EMPTY_ARRAY));
        }

        @Override
        public String toString() {
            return "Node{" +
                    "evaluator=" + evaluator +
                    '}';
        }
    }
}

package org.evrete.runtime;

import org.evrete.runtime.evaluation.BetaEvaluator;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ConditionNodeDescriptor extends NodeDescriptor {
    public static final ConditionNodeDescriptor[] ZERO_ARRAY = new ConditionNodeDescriptor[0];
    private final BetaEvaluator expression;

    private ConditionNodeDescriptor(BetaEvaluator expression, Set<NodeDescriptor> sourceNodes) {
        super(sourceNodes);
        this.expression = expression;
    }

    static Collection<ConditionNodeDescriptor> allocateConditions(Collection<FactType> betaTypes, List<BetaEvaluator> list) {
        final Set<NodeDescriptor> unallocatedNodes = new HashSet<>();
        for (FactType factType : betaTypes) {
            EntryNodeDescriptor e = new EntryNodeDescriptor(factType);
            unallocatedNodes.add(e);
        }

        BetaEvaluator[] evaluatorSequence = list.toArray(new BetaEvaluator[0]);

        // Loop through the expressions one by one
        // The initial order of expressions defines the outcome.
        for (BetaEvaluator evaluator : evaluatorSequence) {
            Set<NodeDescriptor> matching = unallocatedNodes
                    .stream()
                    .filter(new Predicate<NodeDescriptor>() {
                        @Override
                        public boolean test(NodeDescriptor nodeDescriptor) {
                            return nodeDescriptor.getFactTypeMask().intersects(evaluator.getFactTypeMask());
                        }
                    })
                    .collect(Collectors.toSet());

            //Set<NodeDescriptor> matching = Bits.matchesOR(evaluator.getFactTypeMask(), unallocatedNodes, NodeDescriptor::getFactTypeMask);
            assert !matching.isEmpty();
            // replace the matching nodes with a new one
            unallocatedNodes.removeAll(matching);
            unallocatedNodes.add(new ConditionNodeDescriptor(evaluator, matching));
        }

        Collection<ConditionNodeDescriptor> finalNodes = new ArrayList<>(unallocatedNodes.size());
        for (NodeDescriptor nd : unallocatedNodes) {
            if (nd.isConditionNode()) {
                ConditionNodeDescriptor cnd = (ConditionNodeDescriptor) nd;
                finalNodes.add(cnd);
            }
        }
        return finalNodes;
    }

    @Override
    public boolean isConditionNode() {
        return true;
    }

    BetaEvaluator getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return expression.toString();
    }
}

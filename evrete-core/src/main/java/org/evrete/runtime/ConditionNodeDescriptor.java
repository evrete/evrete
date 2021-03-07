package org.evrete.runtime;

import org.evrete.runtime.evaluation.BetaEvaluatorGroup;
import org.evrete.util.Bits;

import java.util.*;

public class ConditionNodeDescriptor extends NodeDescriptor {
    public static final ConditionNodeDescriptor[] ZERO_ARRAY = new ConditionNodeDescriptor[0];
    private final BetaEvaluatorGroup expression;

    private ConditionNodeDescriptor(BetaEvaluatorGroup expression, Set<NodeDescriptor> sourceNodes) {
        super(sourceNodes);
        this.expression = expression;
    }

    static Collection<ConditionNodeDescriptor> allocateConditions(Collection<FactType> betaTypes, List<BetaEvaluatorGroup> list) {
        final Set<NodeDescriptor> unallocatedNodes = new HashSet<>();
        for (FactType factType : betaTypes) {
            EntryNodeDescriptor e = new EntryNodeDescriptor(factType);
            unallocatedNodes.add(e);
        }

        BetaEvaluatorGroup[] evaluatorSequence = list.toArray(BetaEvaluatorGroup.ZERO_ARRAY);

        // Loop through the expressions one by one
        // The initial order of expressions defines the outcome.
        for (BetaEvaluatorGroup evaluator : evaluatorSequence) {
            Set<NodeDescriptor> matching = Bits.matchesOR(evaluator.getTypeMask(), unallocatedNodes, NodeDescriptor::getFactTypeMask);
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

    BetaEvaluatorGroup getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return expression.toString();
    }
}

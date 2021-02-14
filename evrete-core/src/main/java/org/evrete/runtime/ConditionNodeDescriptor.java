package org.evrete.runtime;

import org.evrete.runtime.evaluation.BetaEvaluatorGroup;
import org.evrete.util.Bits;
import org.evrete.util.NextIntSupplier;

import java.util.*;
import java.util.function.Consumer;

public class ConditionNodeDescriptor extends NodeDescriptor {
    public static final ConditionNodeDescriptor[] ZERO_ARRAY = new ConditionNodeDescriptor[0];
    private final BetaEvaluatorGroup expression;

    private ConditionNodeDescriptor(NextIntSupplier idSupplier, BetaEvaluatorGroup expression, Set<NodeDescriptor> sourceNodes) {
        super(idSupplier, sourceNodes);
        this.expression = expression;

        // Set data grouping for each source node
        for (NodeDescriptor src : sourceNodes) {
            Set<FactType> allSourceTypes = new HashSet<>(Arrays.asList(src.getTypes()));
            Set<FactType> conditionTypes = new HashSet<>();
            Set<FactType> descriptor = expression.descriptor();
            for (FactType refType : descriptor) {
                if (allSourceTypes.contains(refType)) {
                    allSourceTypes.remove(refType);
                    conditionTypes.add(refType);
                }
            }

            FactType[] primary = FactType.toArray(conditionTypes);

            FactType[][] conditionGrouping;
            if (allSourceTypes.isEmpty()) {
                conditionGrouping = new FactType[1][];
                conditionGrouping[0] = primary;
            } else {
                FactType[] secondary = FactType.toArray(allSourceTypes);
                conditionGrouping = new FactType[2][];
                conditionGrouping[0] = primary;
                conditionGrouping[1] = secondary;
            }
            src.setEvalGrouping(conditionGrouping);
        }
    }

    static Collection<ConditionNodeDescriptor> allocateConditions(Collection<FactType> betaTypes, List<BetaEvaluatorGroup> list) {
        final Set<NodeDescriptor> unallocatedNodes = new HashSet<>();
        NextIntSupplier idSupplier = new NextIntSupplier();
        for (FactType factType : betaTypes) {
            EntryNodeDescriptor e = new EntryNodeDescriptor(idSupplier, factType);
            unallocatedNodes.add(e);
        }

        BetaEvaluatorGroup[] evaluatorSequence = list.toArray(BetaEvaluatorGroup.ZERO_ARRAY);

        // Loop through the expressions one by one
        // The initial order of expressions defines the outcome.
        for (BetaEvaluatorGroup evaluator : evaluatorSequence) {
            Set<NodeDescriptor> matching = Bits.matchesOR(evaluator.getTypeMask(), unallocatedNodes, NodeDescriptor::getMask);
            assert !matching.isEmpty();
            // replace the matching nodes with a new one
            unallocatedNodes.removeAll(matching);
            unallocatedNodes.add(new ConditionNodeDescriptor(idSupplier, evaluator, matching));
        }
        Collection<ConditionNodeDescriptor> finalNodes = new ArrayList<>(unallocatedNodes.size());
        for (NodeDescriptor nd : unallocatedNodes) {
            if (nd.isConditionNode()) {
                ConditionNodeDescriptor cnd = (ConditionNodeDescriptor) nd;
                cnd.setEvalGrouping(new FactType[][]{cnd.getTypes()});
                finalNodes.add(cnd);
            }
        }
        return finalNodes;
    }

    @Override
    public boolean isConditionNode() {
        return true;
    }

    public BetaEvaluatorGroup getExpression() {
        return expression;
    }

    private static void forEachConditionNode(ConditionNodeDescriptor node, Consumer<ConditionNodeDescriptor> consumer) {
        consumer.accept(node);
        for (NodeDescriptor parent : node.getSources()) {
            if (parent.isConditionNode()) {
                forEachConditionNode((ConditionNodeDescriptor) parent, consumer);
            }
        }
    }


    void forEachConditionNode(Consumer<ConditionNodeDescriptor> consumer) {
        forEachConditionNode(this, consumer);
    }


    @Override
    public String toString() {
        return "{id=" + getIndex() +
                ", expression=" + expression +
                ", mask=" + getMask() +
                '}';
    }
}
